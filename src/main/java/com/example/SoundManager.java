package com.example;

import javafx.scene.media.AudioClip;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {

    private static final Map<String, AudioClip> cache = new HashMap<>();
    private static boolean enabled = true;

    public static void setEnabled(boolean e) { enabled = e; }

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
