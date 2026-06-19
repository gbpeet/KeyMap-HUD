package com.itinerant.keymaphud.client;

import org.lwjgl.glfw.GLFW;
import java.util.List;

public final class KeyboardLayout {
    private KeyboardLayout() {}

    public static List<KeyVisual> ansiFull() {
        return List.of(
                // Function row
                key(GLFW.GLFW_KEY_ESCAPE, "ESC", 0, 0),
                key(GLFW.GLFW_KEY_F1, "F1", 56, 0),
                key(GLFW.GLFW_KEY_F2, "F2", 88, 0),
                key(GLFW.GLFW_KEY_F3, "F3", 120, 0),
                key(GLFW.GLFW_KEY_F4, "F4", 152, 0),
                key(GLFW.GLFW_KEY_F5, "F5", 200, 0),
                key(GLFW.GLFW_KEY_F6, "F6", 232, 0),
                key(GLFW.GLFW_KEY_F7, "F7", 264, 0),
                key(GLFW.GLFW_KEY_F8, "F8", 296, 0),
                key(GLFW.GLFW_KEY_F9, "F9", 344, 0),
                key(GLFW.GLFW_KEY_F10, "F10", 376, 0),
                key(GLFW.GLFW_KEY_F11, "F11", 408, 0),
                key(GLFW.GLFW_KEY_F12, "F12", 440, 0),

                // Number row
                key(GLFW.GLFW_KEY_GRAVE_ACCENT, "`", 0, 36),
                key(GLFW.GLFW_KEY_1, "1", 32, 36),
                key(GLFW.GLFW_KEY_2, "2", 64, 36),
                key(GLFW.GLFW_KEY_3, "3", 96, 36),
                key(GLFW.GLFW_KEY_4, "4", 128, 36),
                key(GLFW.GLFW_KEY_5, "5", 160, 36),
                key(GLFW.GLFW_KEY_6, "6", 192, 36),
                key(GLFW.GLFW_KEY_7, "7", 224, 36),
                key(GLFW.GLFW_KEY_8, "8", 256, 36),
                key(GLFW.GLFW_KEY_9, "9", 288, 36),
                key(GLFW.GLFW_KEY_0, "0", 320, 36),
                key(GLFW.GLFW_KEY_MINUS, "-", 352, 36),
                key(GLFW.GLFW_KEY_EQUAL, "=", 384, 36),
                new KeyVisual(GLFW.GLFW_KEY_BACKSPACE, "BACK", 416, 36, 56, 22),

                // QWERTY row
                new KeyVisual(GLFW.GLFW_KEY_TAB, "TAB", 0, 66, 42, 22),
                key(GLFW.GLFW_KEY_Q, "Q", 46, 66),
                key(GLFW.GLFW_KEY_W, "W", 78, 66),
                key(GLFW.GLFW_KEY_E, "E", 110, 66),
                key(GLFW.GLFW_KEY_R, "R", 142, 66),
                key(GLFW.GLFW_KEY_T, "T", 174, 66),
                key(GLFW.GLFW_KEY_Y, "Y", 206, 66),
                key(GLFW.GLFW_KEY_U, "U", 238, 66),
                key(GLFW.GLFW_KEY_I, "I", 270, 66),
                key(GLFW.GLFW_KEY_O, "O", 302, 66),
                key(GLFW.GLFW_KEY_P, "P", 334, 66),
                key(GLFW.GLFW_KEY_LEFT_BRACKET, "[", 366, 66),
                key(GLFW.GLFW_KEY_RIGHT_BRACKET, "]", 398, 66),
                new KeyVisual(GLFW.GLFW_KEY_BACKSLASH, "\\", 430, 66, 42, 22),

                // Home row
                new KeyVisual(GLFW.GLFW_KEY_CAPS_LOCK, "CAPS", 0, 96, 50, 22),
                key(GLFW.GLFW_KEY_A, "A", 54, 96),
                key(GLFW.GLFW_KEY_S, "S", 86, 96),
                key(GLFW.GLFW_KEY_D, "D", 118, 96),
                key(GLFW.GLFW_KEY_F, "F", 150, 96),
                key(GLFW.GLFW_KEY_G, "G", 182, 96),
                key(GLFW.GLFW_KEY_H, "H", 214, 96),
                key(GLFW.GLFW_KEY_J, "J", 246, 96),
                key(GLFW.GLFW_KEY_K, "K", 278, 96),
                key(GLFW.GLFW_KEY_L, "L", 310, 96),
                key(GLFW.GLFW_KEY_SEMICOLON, ";", 342, 96),
                key(GLFW.GLFW_KEY_APOSTROPHE, "'", 374, 96),
                new KeyVisual(GLFW.GLFW_KEY_ENTER, "ENTER", 406, 96, 66, 22),

                // Bottom letter row
                new KeyVisual(GLFW.GLFW_KEY_LEFT_SHIFT, "SHIFT", 0, 126, 66, 22),
                key(GLFW.GLFW_KEY_Z, "Z", 70, 126),
                key(GLFW.GLFW_KEY_X, "X", 102, 126),
                key(GLFW.GLFW_KEY_C, "C", 134, 126),
                key(GLFW.GLFW_KEY_V, "V", 166, 126),
                key(GLFW.GLFW_KEY_B, "B", 198, 126),
                key(GLFW.GLFW_KEY_N, "N", 230, 126),
                key(GLFW.GLFW_KEY_M, "M", 262, 126),
                key(GLFW.GLFW_KEY_COMMA, ",", 294, 126),
                key(GLFW.GLFW_KEY_PERIOD, ".", 326, 126),
                key(GLFW.GLFW_KEY_SLASH, "/", 358, 126),
                new KeyVisual(GLFW.GLFW_KEY_RIGHT_SHIFT, "SHIFT", 390, 126, 82, 22),

                // Modifier row
                new KeyVisual(GLFW.GLFW_KEY_LEFT_CONTROL, "CTRL", 0, 156, 42, 22),
                new KeyVisual(GLFW.GLFW_KEY_LEFT_SUPER, "WIN", 46, 156, 42, 22),
                new KeyVisual(GLFW.GLFW_KEY_LEFT_ALT, "ALT", 92, 156, 42, 22),
                new KeyVisual(GLFW.GLFW_KEY_SPACE, "SPACE", 138, 156, 170, 22),
                new KeyVisual(GLFW.GLFW_KEY_RIGHT_ALT, "ALT", 312, 156, 42, 22),
                new KeyVisual(GLFW.GLFW_KEY_RIGHT_SUPER, "WIN", 358, 156, 42, 22),
                new KeyVisual(GLFW.GLFW_KEY_RIGHT_CONTROL, "CTRL", 404, 156, 68, 22),

                // Mouse cluster
                new KeyVisual(-100, "LMB", -110, 66, 42, 42),
                new KeyVisual(-99, "RMB", -64, 66, 42, 42),

                new KeyVisual(-98, "MMB", -87, 112, 42, 22),

                new KeyVisual(-97, "MB4", -110, 138, 42, 22),
                new KeyVisual(-96, "MB5", -64, 138, 42, 22),

                // Navigation cluster
                key(GLFW.GLFW_KEY_INSERT, "INS", 504, 36),
                key(GLFW.GLFW_KEY_HOME, "HOME", 536, 36),
                key(GLFW.GLFW_KEY_PAGE_UP, "PGU", 568, 36),
                key(GLFW.GLFW_KEY_DELETE, "DEL", 504, 66),
                key(GLFW.GLFW_KEY_END, "END", 536, 66),
                key(GLFW.GLFW_KEY_PAGE_DOWN, "PGD", 568, 66),

                key(GLFW.GLFW_KEY_UP, "↑", 536, 126),
                key(GLFW.GLFW_KEY_LEFT, "←", 504, 156),
                key(GLFW.GLFW_KEY_DOWN, "↓", 536, 156),
                key(GLFW.GLFW_KEY_RIGHT, "→", 568, 156),

                // Numpad
                key(GLFW.GLFW_KEY_NUM_LOCK, "NUM", 632, 36),
                key(GLFW.GLFW_KEY_KP_DIVIDE, "/", 664, 36),
                key(GLFW.GLFW_KEY_KP_MULTIPLY, "*", 696, 36),
                key(GLFW.GLFW_KEY_KP_SUBTRACT, "-", 728, 36),

                key(GLFW.GLFW_KEY_KP_7, "7", 632, 66),
                key(GLFW.GLFW_KEY_KP_8, "8", 664, 66),
                key(GLFW.GLFW_KEY_KP_9, "9", 696, 66),
                new KeyVisual(GLFW.GLFW_KEY_KP_ADD, "+", 728, 66, 28, 52),

                key(GLFW.GLFW_KEY_KP_4, "4", 632, 96),
                key(GLFW.GLFW_KEY_KP_5, "5", 664, 96),
                key(GLFW.GLFW_KEY_KP_6, "6", 696, 96),

                key(GLFW.GLFW_KEY_KP_1, "1", 632, 126),
                key(GLFW.GLFW_KEY_KP_2, "2", 664, 126),
                key(GLFW.GLFW_KEY_KP_3, "3", 696, 126),
                new KeyVisual(GLFW.GLFW_KEY_KP_ENTER, "ENT", 728, 126, 28, 52),

                new KeyVisual(GLFW.GLFW_KEY_KP_0, "0", 632, 156, 60, 22),
                key(GLFW.GLFW_KEY_KP_DECIMAL, ".", 696, 156)
        );
    }

    private static KeyVisual key(int keyCode, String label, int x, int y) {
        return new KeyVisual(keyCode, label, x, y, 28, 22);
    }

    public record KeyVisual(int keyCode, String label, int x, int y, int width, int height) {
    }
}