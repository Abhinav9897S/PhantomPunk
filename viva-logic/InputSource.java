/**
 * Where the next command comes from.
 *
 * OOP: INTERFACE POLYMORPHISM - the Game asks "what next?" and does not care whether a
 *      person is typing (KeyboardInput) or the computer is playing (AutoPilot).
 */
public interface InputSource {
    Command nextCommand(Game game);
}
