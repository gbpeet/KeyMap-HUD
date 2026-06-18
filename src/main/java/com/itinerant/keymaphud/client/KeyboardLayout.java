package com.itinerant.keymaphud.client;

import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class KeyboardLayout {
    private KeyboardLayout() {
    }

    public static List<KeyVisual> ansiLettersOnly() {
        return List.of(
                new KeyVisual(GLFW.GLFW_KEY_Q, "Q", 0, 0, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_W, "W", 32, 0, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_E, "E", 64, 0, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_R, "R", 96, 0, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_T, "T", 128, 0, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_Y, "Y", 160, 0, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_U, "U", 192, 0, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_I, "I", 224, 0, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_O, "O", 256, 0, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_P, "P", 288, 0, 28, 22),

                new KeyVisual(GLFW.GLFW_KEY_A, "A", 14, 30, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_S, "S", 46, 30, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_D, "D", 78, 30, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_F, "F", 110, 30, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_G, "G", 142, 30, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_H, "H", 174, 30, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_J, "J", 206, 30, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_K, "K", 238, 30, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_L, "L", 270, 30, 28, 22),

                new KeyVisual(GLFW.GLFW_KEY_Z, "Z", 42, 60, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_X, "X", 74, 60, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_C, "C", 106, 60, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_V, "V", 138, 60, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_B, "B", 170, 60, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_N, "N", 202, 60, 28, 22),
                new KeyVisual(GLFW.GLFW_KEY_M, "M", 234, 60, 28, 22),

                new KeyVisual(GLFW.GLFW_KEY_SPACE, "SPACE", 112, 96, 168, 22)
        );
    }

    public record KeyVisual(int keyCode, String label, int x, int y, int width, int height) {
    }
}