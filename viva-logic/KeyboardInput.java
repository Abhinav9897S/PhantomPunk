import java.util.Scanner;

/** Reads commands typed by a person.   OOP: one implementation of InputSource. */
public class KeyboardInput implements InputSource {
    private final Scanner scanner;

    public KeyboardInput(Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public Command nextCommand(Game game) {
        while (true) {
            StringBuilder menu = new StringBuilder("Your move -");
            for (Command c : Command.values()) menu.append("  ").append(c.getKey()).append(": ").append(c.getDescription());
            System.out.println(menu);
            System.out.print("> ");
            if (!scanner.hasNextLine()) return Command.QUIT;
            Command command = Command.fromKey(scanner.nextLine());
            if (command != null) return command;
            System.out.println("Unknown key, try again.");
        }
    }
}
