package phantom.screens;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import phantom.input.ActionInput;

/**
 * A stack of screens: Title, Game, Pause, Map, Dialogue, Death... Pushing an overlay pauses
 * the game underneath while it keeps being drawn.
 * <p>OOP concepts: {@link Deque} used as a stack; <b>State pattern</b> for game modes;
 * polymorphic calls to {@link BaseScreen#update} and {@link BaseScreen#render}.</p>
 */
public final class ScreenManager {
    private final Deque<BaseScreen> stack = new ArrayDeque<>();
    private final ActionInput input;
    private final List<BaseScreen> renderList = new ArrayList<>();
    private int width;
    private int height;

    public ScreenManager(ActionInput input) {
        this.input = input;
    }

    public void push(BaseScreen screen) {
        stack.push(screen);
        input.consumeAll();
        screen.resize(width, height);
        screen.onEnter();
    }

    /** Removes and disposes the top screen. */
    public void pop() {
        BaseScreen top = stack.poll();
        if (top != null) {
            top.onExit();
            top.dispose();
        }
        input.consumeAll();
    }

    /** Clears the whole stack and starts again with one screen. */
    public void replaceAll(BaseScreen screen) {
        while (!stack.isEmpty()) pop();
        push(screen);
    }

    public void update(float dt) {
        BaseScreen top = stack.peek();
        if (top == null) return;
        top.update(dt);
        if (top.updatesBelow() && stack.size() > 1) {
            Iterator<BaseScreen> it = stack.iterator();
            it.next();
            BaseScreen below = it.next();
            if (below != top) below.update(dt);
        }
    }

    public void render(float alpha, float frameDelta) {
        renderList.clear();
        for (BaseScreen s : stack) {
            renderList.add(s);
            if (!s.isOverlay()) break;
        }
        for (int i = renderList.size() - 1; i >= 0; i--) {
            renderList.get(i).render(alpha, frameDelta);
        }
    }

    public void resize(int w, int h) {
        width = w;
        height = h;
        for (BaseScreen s : stack) s.resize(w, h);
    }

    public BaseScreen top() { return stack.peek(); }
    public int size() { return stack.size(); }

    public void dispose() {
        while (!stack.isEmpty()) pop();
    }
}
