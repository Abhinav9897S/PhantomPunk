import java.nio.file.Path;
import java.util.Scanner;

/**
 * Entry point: a small text menu.
 *
 *   javac *.java
 *   java Main            (menu)
 *   java Main --autoplay (watch the computer play to the end)
 */
public class Main {
    public static void main(String[] args) {
        SaveManager saves = new SaveManager(Path.of("phantom-save.txt"));
        if (args.length > 0 && args[0].equals("--autoplay")) {
            new Game(new AutoPilot(), saves, true).play();
            return;
        }
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println();
            System.out.println("=== PHANTOM: The Umbral Descent (logic edition) ===");
            System.out.println("1. New Game   2. Continue   3. Autoplay   4. Exit");
            System.out.print("> ");
            if (!scanner.hasNextLine()) return;
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    new Game(new KeyboardInput(scanner), saves, false).play();
                    break;
                case "2":
                    try {
                        Game game = new Game(new KeyboardInput(scanner), saves, false);
                        game.loadFrom(saves.load());
                        game.play();
                    } catch (SaveException e) {   // checked exception: we MUST handle it
                        System.out.println("Cannot continue: " + e.getMessage());
                    }
                    break;
                case "3":
                    new Game(new AutoPilot(), saves, true).play();
                    break;
                case "4":
                    System.out.println("Goodbye.");
                    return;
                default:
                    System.out.println("Please type 1, 2, 3 or 4.");
            }
        }
    }
}
