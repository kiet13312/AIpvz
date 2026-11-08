package com.alpvz.gardendefense;

/** 20 thế giới (theo danh sách thế giới PvZ2 trên Wiki) x 10 map = 200 map. */
final class Worlds {
    static final int COUNT = 20;
    // cơ chế đặc trưng của thế giới
    static final int G_NONE = 0, G_GRAVE = 1, G_FOG = 2, G_SAND = 3, G_POWER = 4, G_GOLD = 5, G_WIND = 6;
    // kiểu trang trí nền: 0 bụi cây, 1 kim tự tháp/núi, 2 sóng, 3 xương rồng, 4 tòa nhà/neon, 5 núi tuyết
    static final String[] NAME = {"Sân Vườn Nhà", "Ai Cập Cổ Đại", "Biển Hải Tặc", "Miền Tây Hoang Dã", "Thế Giới Công Phu",
            "Tương Lai Xa", "Thời Trung Cổ", "Bãi Biển Sóng Lớn", "Hang Băng Giá", "Thành Phố Trên Mây",
            "Thành Cổ Thất Lạc", "Neon Mixtape Tour", "Đầm Lầy Kỷ Jura", "Thời Hiện Đại", "Thời Đại Hơi Nước",
            "Thời Phục Hưng", "Thời Heian", "Thế Giới Dưới Nước", "La Mã Cổ Đại", "Rạp Xiếc Big Top"};
    static final int[] SKY_TOP = {0xFF8ED8F6, 0xFFF6C873, 0xFF5FB4E8, 0xFFF0A860, 0xFFE06A5A, 0xFF1C2A5E, 0xFF2A2F4F, 0xFF4FC8F0, 0xFFB8E4F8, 0xFF9AD0FF,
            0xFF3E7A4A, 0xFF5A2A8A, 0xFF7AA860, 0xFF6AA8E0, 0xFFB09A80, 0xFFC09070, 0xFFF4B6C8, 0xFF1E78B8, 0xFFE8A890, 0xFFE05A78};
    static final int[] SKY_BOT = {0xFFD8F0BE, 0xFFFFE6A8, 0xFFB8E8F8, 0xFFFFD8A0, 0xFFFFC8A0, 0xFF4A6AA8, 0xFF5A5F7F, 0xFFC8F0FF, 0xFFF0FAFF, 0xFFF0F8FF,
            0xFF8AC890, 0xFFE05AC8, 0xFFC8E0A0, 0xFFD0E8F8, 0xFFD8C8A8, 0xFFE8C8A8, 0xFFFFE0E8, 0xFF58B8D8, 0xFFFFD8C0, 0xFFFFB0C0};
    static final int[] GRASS_A = {0xFF7DC24B, 0xFFD9B864, 0xFF5DB06A, 0xFFC8924A, 0xFF9A6A3A, 0xFF4A6AA8, 0xFF4F7A48, 0xFFE8D090, 0xFFA8D8E8, 0xFF9AD070,
            0xFF6A9A50, 0xFF8A4AB8, 0xFF6AA046, 0xFF7AB650, 0xFF9A8A6A, 0xFF8AA060, 0xFFE0A0B8, 0xFF3A98B8, 0xFFC8A070, 0xFFD87A58};
    static final int[] GRASS_B = {0xFF72B840, 0xFFCDAA58, 0xFF4FA05C, 0xFFBA853E, 0xFF8C5C30, 0xFF3C5A98, 0xFF446C3E, 0xFFDCC484, 0xFF98C8DC, 0xFF8AC264,
            0xFF5C8C44, 0xFF7C3EA8, 0xFF5C9440, 0xFF6EA846, 0xFF8C7C5C, 0xFF7C9254, 0xFFD494AC, 0xFF3088A8, 0xFFBA9464, 0xFFCA6C4C};
    static final int[] HEDGE = {0xFF2F7A30, 0xFF9A7A34, 0xFF2A6A58, 0xFF8A5A28, 0xFF7A3A2A, 0xFF22346E, 0xFF22262E, 0xFF2A8AA8, 0xFF6AA0B8, 0xFF7AA8D8,
            0xFF2A5A34, 0xFF4A1E70, 0xFF3A6A30, 0xFF3A6A98, 0xFF6A5A44, 0xFF6A4A38, 0xFFB0708A, 0xFF16608A, 0xFF9A6A58, 0xFFA03A56};
    static final int[] DECOR = {0, 1, 2, 3, 1, 4, 0, 2, 5, 2, 1, 4, 0, 4, 4, 1, 0, 2, 1, 4};
    static final int[] GIMMICK = {G_NONE, G_SAND, G_GRAVE, G_GRAVE, G_GOLD, G_POWER, G_FOG, G_WIND, G_GRAVE, G_WIND,
            G_GOLD, G_POWER, G_FOG, G_NONE, G_GOLD, G_FOG, G_WIND, G_FOG, G_GRAVE, G_POWER};
    static final String[] GIMMICK_NAME = {"", "Bia mộ chắn đường", "Sương mù", "Bão cát tăng tốc zombie", "Ô năng lượng bắn nhanh +50%",
            "Ô vàng tặng nắng", "Gió thổi lùi zombie"};

    private Worlds() {}

    static String name(int w) { return NAME[w]; }
    static int skyTop(int w) { return SKY_TOP[w]; }
    static int skyBot(int w) { return SKY_BOT[w]; }
    static int grassA(int w) { return GRASS_A[w]; }
    static int grassB(int w) { return GRASS_B[w]; }
    static int hedge(int w) { return HEDGE[w]; }
    static int decor(int w) { return DECOR[w]; }
    static int gimmick(int w) { return GIMMICK[w]; }
    static float spd(int w) { return 1f + w * .012f; }
    static float hpm(int w) { return 1f + w * .03f; }
}
