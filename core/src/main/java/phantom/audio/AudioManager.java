package phantom.audio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;

import phantom.events.EventBus;
import phantom.events.Events;
import phantom.save.Settings;
import phantom.world.Region;

/**
 * Plays synthesised sound effects and region ambience. It listens to the event bus, so
 * gameplay code only publishes {@code Events.Sfx}; it never calls the audio system directly.
 * Any audio failure (no device, no backend) disables sound instead of crashing.
 * <p>OOP concepts: <b>Observer</b> (subscribes to events), {@link EnumMap} of sounds,
 * exception handling, {@link Disposable} resources.</p>
 */
public final class AudioManager implements Disposable {
    private static final float AMBIENT_SECONDS = 8f;

    private final Map<SoundId, Sound> sounds = new EnumMap<>(SoundId.class);
    private final Map<Region, Music> ambience = new EnumMap<>(Region.class);
    private final List<Runnable> subscriptions = new ArrayList<>();
    private final Settings settings;
    private Path tempDir;
    private Music current;
    private boolean available;

    public AudioManager(Settings settings) {
        this.settings = settings;
        try {
            tempDir = Files.createTempDirectory("phantom-audio");
            tempDir.toFile().deleteOnExit();
            for (SoundId id : SoundId.values()) {
                sounds.put(id, Gdx.audio.newSound(Gdx.files.absolute(write(id.name(), SoundSynth.effect(id)).toString())));
            }
            for (Region r : Region.values()) {
                Music m = Gdx.audio.newMusic(Gdx.files.absolute(write("amb_" + r.name(), SoundSynth.ambient(r, AMBIENT_SECONDS)).toString()));
                m.setLooping(true);
                ambience.put(r, m);
            }
            available = true;
        } catch (Throwable t) {
            available = false;
            if (Gdx.app != null) Gdx.app.error("Audio", "Audio disabled: " + t);
        }
    }

    private Path write(String name, float[] samples) throws IOException {
        Path file = tempDir.resolve(name + ".wav");
        Files.write(file, SoundSynth.toWav(samples));
        file.toFile().deleteOnExit();
        return file;
    }

    /** Subscribes to a game session's events; call {@link #detach()} when the session ends. */
    public void attach(EventBus bus) {
        detach();
        subscriptions.add(bus.subscribe(Events.Sfx.class, e -> play(e.sound())));
        subscriptions.add(bus.subscribe(Events.RoomEntered.class, e -> playAmbience(e.region())));
    }

    public void detach() {
        for (Runnable r : subscriptions) r.run();
        subscriptions.clear();
    }

    public void play(SoundId id) {
        if (!available) return;
        Sound s = sounds.get(id);
        if (s == null) return;
        try {
            float pitch = 0.94f + (float) Math.random() * 0.12f;
            s.play(settings.getSfxVolume(), pitch, 0f);
        } catch (Throwable ignored) {
            // a failed effect is never worth a crash
        }
    }

    public void playAmbience(Region region) {
        if (!available) return;
        Music next = ambience.get(region);
        if (next == current) return;
        try {
            if (current != null) current.stop();
            current = next;
            if (current != null) {
                current.setVolume(settings.getMusicVolume() * 0.8f);
                current.play();
            }
        } catch (Throwable t) {
            available = false;
        }
    }

    public void stopAmbience() {
        if (current != null && available) current.stop();
        current = null;
    }

    public void applyVolumes() {
        if (current != null && available) current.setVolume(settings.getMusicVolume() * 0.8f);
    }

    public boolean isAvailable() { return available; }

    @Override
    public void dispose() {
        detach();
        for (Sound s : sounds.values()) s.dispose();
        for (Music m : ambience.values()) m.dispose();
        sounds.clear();
        ambience.clear();
    }
}
