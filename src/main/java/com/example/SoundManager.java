package com.example;

import javafx.scene.media.AudioClip;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {

    private static final Map<String, AudioClip> cache = new HashMap<>();
    private static boolean enabled = true;

    private static javafx.scene.media.MediaPlayer musicPlayer;
    private static String currentMusic = null;
    private static boolean musicEnabled = true;

    public static void setEnabled(boolean e) { enabled = e; }

    public static void setMusicEnabled(boolean e) {
        musicEnabled = e;
        if (!e) stopMusic();
    }

    public static void playMusic(String name) {
        if (!musicEnabled) return;
        if (name.equals(currentMusic) && musicPlayer != null) return;

        stopMusic();
        try {
            java.net.URL url = SoundManager.class.getResource(
                "/assets/musicas/" + name + ".mp3");
            if (url == null) {
                System.out.println("Música não encontrada: " + name);
                return;
            }
            javafx.scene.media.Media media =
                new javafx.scene.media.Media(url.toExternalForm());
            musicPlayer = new javafx.scene.media.MediaPlayer(media);
            musicPlayer.setCycleCount(javafx.scene.media.MediaPlayer.INDEFINITE);
            musicPlayer.setVolume(0.35);
            musicPlayer.play();
            currentMusic = name;
        } catch (Exception ex) {
            System.out.println("Erro ao tocar música " + name + ": " + ex.getMessage());
        }
    }

    public static void stopMusic() {
        if (musicPlayer != null) {
            try { musicPlayer.stop(); musicPlayer.dispose(); } catch (Exception ignored) {}
            musicPlayer = null;
            currentMusic = null;
        }
    }

    public static void play(String name) {
        if (!enabled) return;
        try {
            AudioClip clip = cache.get(name);
            if (clip == null) {
                java.net.URL url = SoundManager.class.getResource(
                    "/assets/sounds/" + name + ".mp3");
                if (url == null) {
                    System.out.println("Som não encontrado: " + name);
                    return;
                }
                clip = new AudioClip(url.toExternalForm());
                cache.put(name, clip);
            }
            clip.play();
        } catch (Exception ex) {
            System.out.println("Erro ao tocar som " + name + ": " + ex.getMessage());
        }
    }
}
