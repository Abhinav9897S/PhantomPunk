package phantom.save;

/**
 * Player-adjustable options, stored in their own file so they survive a New Game.
 * <p>OOP concept: encapsulation with validating setters.</p>
 */
public final class Settings {
    private float musicVolume = 0.6f;
    private float sfxVolume = 0.8f;
    private boolean screenShake = true;
    private boolean fullscreen;

    public float getMusicVolume() { return musicVolume; }
    public void setMusicVolume(float v) { musicVolume = clamp(v); }
    public float getSfxVolume() { return sfxVolume; }
    public void setSfxVolume(float v) { sfxVolume = clamp(v); }
    public boolean isScreenShake() { return screenShake; }
    public void setScreenShake(boolean on) { screenShake = on; }
    public boolean isFullscreen() { return fullscreen; }
    public void setFullscreen(boolean on) { fullscreen = on; }

    private static float clamp(float v) {
        if (Float.isNaN(v)) return 0.5f;
        return Math.max(0f, Math.min(1f, v));
    }

    /** Repairs values loaded from a hand-edited file. */
    void sanitize() {
        musicVolume = clamp(musicVolume);
        sfxVolume = clamp(sfxVolume);
    }
}
