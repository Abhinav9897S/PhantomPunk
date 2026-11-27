import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The game itself: the rooms, the player and the turn loop.
 *
 * One TURN = 1) the player chooses a command (from an InputSource),
 *            2) the command is carried out,
 *            3) every living enemy takes its turn,
 *            4) dead enemies give shards; beaten bosses give abilities;
 *            5) if the player died, they wake up at the last bench.
 *
 * OOP: COMPOSITION - Game HAS-A Player, a list of Rooms, an EventBus, a SaveManager.
 * OOP: OBSERVER - the constructor subscribes lambdas to events.
 * OOP: POLYMORPHISM - "input" can be a KeyboardInput or an AutoPilot.
 */
public final class Game {
    private static final int MAX_TURNS = 3000;   // safety limit for autoplay

    private final List<Room> rooms = World.create();
    private final Player player = new Player();
    private final EventBus events = new EventBus();
    private final Set<String> defeatedBosses = new LinkedHashSet<>();
    private final InputSource input;
    private final SaveManager saves;
    private final boolean quiet;
    private int currentRoom;
    private int respawnRoom;
    private int turn;
    private boolean running = true;
    private boolean won;

    public Game(InputSource input, SaveManager saves, boolean quiet) {
        this.input = input;
        this.saves = saves;
        this.quiet = quiet;
        new Achievements(events, this);
        // Observer: react to events with lambdas
        events.subscribe(GameEvent.EnemyKilled.class, e -> {
            player.getInventory().addShards(e.shards());
            log(e.enemyName() + " is defeated. +" + e.shards() + " shards.");
        });
        events.subscribe(GameEvent.BossDefeated.class, e -> log("=== " + e.bossName() + " falls! ==="));
        events.subscribe(GameEvent.AbilityUnlocked.class, e ->
                log("New ability: " + e.ability().getDisplayName() + " - " + e.ability().getHint()));
        events.subscribe(GameEvent.BenchRested.class, e -> log("You rest on the bench. Health restored; you will wake here."));
        events.subscribe(GameEvent.PlayerDied.class, e -> log("Your light fades in " + e.roomName() + "... you wake at the bench."));
    }

    /** Restores a saved game (used by "Continue"). */
    public void loadFrom(SaveData data) {
        for (AbilityType a : data.abilities()) player.unlock(a);
        player.getHealth().healFull();
        player.takeDamage(player.getHealth().getMax() - data.health());
        player.setSoul(data.soul());
        player.getInventory().addShards(data.shards());
        defeatedBosses.addAll(data.defeatedBosses());
        for (Room room : rooms) {
            for (GameObject o : room.getObjects()) {
                if (o instanceof Boss boss && defeatedBosses.contains(boss.getName())) room.removeObject(boss);
            }
        }
        respawnRoom = data.respawnRoom();
        currentRoom = respawnRoom;
    }

    /** The main loop. Returns true if the game was finished. */
    public boolean play() {
        log("You descend into the dark beneath Veyra...");
        while (running && turn < MAX_TURNS) {
            showStatus();
            Command command = input.nextCommand(this);
            if (quiet) log("> " + command.getDescription());
            player.setDodging(false);
            boolean tookTurn = perform(command);
            if (tookTurn && running) {
                enemiesAct();
                collectDefeated();
                checkDeath();
            }
            turn++;
        }
        return won;
    }

    /** Carries out one command. Returns false for commands that cost no time (save, quit, invalid move). */
    private boolean perform(Command command) {
        Room room = getCurrentRoom();
        List<Enemy> enemies = room.getLivingEnemies();
        Enemy target = enemies.isEmpty() ? null : enemies.get(0);
        switch (command) {
            case ATTACK:
                log(target == null ? "You swing at the empty air." : player.attack(target));
                return true;
            case BOLT:
                log(player.useAbility(AbilityType.LIGHT_BOLT, target));
                return true;
            case FOCUS:
                log(player.useAbility(AbilityType.FOCUS, null));
                return true;
            case DASH:
                log(player.useAbility(AbilityType.DASH, null));
                return true;
            case INTERACT:
                Interactable thing = room.nextInteractable();
                if (thing == null) log("There is nothing here to use.");
                else thing.interact(player, this);
                return true;
            case NEXT_ROOM:
                moveToNextRoom(enemies);
                return false;
            case SAVE:
                save();
                return false;
            default:   // QUIT
                running = false;
                return false;
        }
    }

    private void moveToNextRoom(List<Enemy> enemies) {
        if (!enemies.isEmpty()) {
            log("You cannot leave while " + enemies.get(0).getName() + " blocks the way.");
            return;
        }
        if (currentRoom + 1 >= rooms.size()) {
            log("There is nowhere further to go.");
            return;
        }
        Room next = rooms.get(currentRoom + 1);
        if (!next.canEnter(player)) {   // ability gate
            log(next.getGateText());
            return;
        }
        currentRoom++;
        log("");
        log("---- " + next.getName() + " ----");
    }

    private void enemiesAct() {
        for (Enemy enemy : getCurrentRoom().getLivingEnemies()) {
            String message = enemy.takeTurn(player);   // each enemy behaves in its own way
            if (message != null) log(message);
            if (!player.isAlive()) return;
        }
    }

    /** Removes dead enemies, pays shards, and hands out boss rewards. */
    private void collectDefeated() {
        Room room = getCurrentRoom();
        for (GameObject o : room.getObjects()) {
            if (!(o instanceof Enemy enemy) || enemy.isAlive()) continue;
            room.removeObject(enemy);
            events.publish(new GameEvent.EnemyKilled(enemy.getName(), enemy.getShardReward()));
            if (enemy instanceof Boss boss) {
                defeatedBosses.add(boss.getName());
                events.publish(new GameEvent.BossDefeated(boss.getName(), boss.isFinal()));
                if (boss.getReward() != null && player.unlock(boss.getReward())) {
                    events.publish(new GameEvent.AbilityUnlocked(boss.getReward()));
                }
                if (boss.isFinal()) finish();
            }
        }
    }

    private void checkDeath() {
        if (player.isAlive()) return;
        events.publish(new GameEvent.PlayerDied(getCurrentRoom().getName()));
        getCurrentRoom().resetEnemies();
        currentRoom = respawnRoom;
        player.getHealth().healFull();
        player.setSoul(0);
    }

    private void finish() {
        won = true;
        running = false;
        log("");
        log("The Heart lies still. The Mist turns to light, and morning reaches the Descent.");
        log("THE END - finished in " + (turn + 1) + " turns with " + player.getInventory().getShards() + " shards.");
    }

    private void save() {
        SaveData data = new SaveData(respawnRoom, player.getHealth().getCurrent(), player.getSoul(),
                player.getInventory().getShards(), Set.copyOf(abilityList()), Set.copyOf(defeatedBosses));
        try {
            saves.save(data);
            log("Game saved (you will continue from the last bench).");
        } catch (SaveException e) {
            log("Could not save: " + e.getMessage());
        }
    }

    private List<AbilityType> abilityList() {
        return java.util.Arrays.stream(AbilityType.values()).filter(player::hasAbility).toList();
    }

    private void showStatus() {
        if (quiet) return;
        Room room = getCurrentRoom();
        System.out.println();
        System.out.println("[" + room.getName() + "]  " + player.describe());
        for (GameObject o : room.getObjects()) System.out.println("   - " + o.describe());   // polymorphic describe()
    }

    public void log(String message) {
        System.out.println(message);
    }

    // getters used by AutoPilot and the props
    public Player getPlayer() { return player; }
    public Room getCurrentRoom() { return rooms.get(currentRoom); }
    public int getCurrentRoomIndex() { return currentRoom; }
    public void setRespawnRoom(int room) { respawnRoom = room; }
    public EventBus getEvents() { return events; }
    public int getTurn() { return turn; }
}
