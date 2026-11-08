package com.alpvz.gardendefense;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Toàn bộ hình ảnh cây / đạn / nắng / máy cắt cỏ được vẽ bằng vector (Canvas) theo phong cách PvZ2.
 * Không tạo object mới trong lúc vẽ (tái sử dụng RectF + Path) để giữ 60 FPS.
 * Hệ toạ độ thiết kế: ô 100x100, gốc (0,0) ở giữa ô, y hướng xuống.
 */
final class Art {
    static final int SUN = 1, PEA = 2, GIGA = 3, CHOMP = 4, REP = 5, MINE = 6, BINU = 7,
            CAB = 8, KER = 9, MEL = 10;
    // kiểu đạn ném
    static final int A_CAB = 0, A_KER = 1, A_BUTTER = 2, A_MEL = 3;

    static final int OUT = 0xFF1F3314;
    private static final RectF R = new RectF();
    private static final Path P = new Path();

    private Art() {}

    // ---------- primitives ----------
    static void oval(Canvas c, Paint p, float cx, float cy, float rx, float ry, int fill) {
        R.set(cx - rx, cy - ry, cx + rx, cy + ry);
        p.setStyle(Paint.Style.FILL);
        p.setColor(fill);
        c.drawOval(R, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.6f);
        p.setColor(OUT);
        c.drawOval(R, p);
        p.setStyle(Paint.Style.FILL);
    }

    static void fill(Canvas c, Paint p, float cx, float cy, float rx, float ry, int col) {
        R.set(cx - rx, cy - ry, cx + rx, cy + ry);
        p.setStyle(Paint.Style.FILL);
        p.setColor(col);
        c.drawOval(R, p);
    }

    static void shine(Canvas c, Paint p, float cx, float cy, float rx, float ry) {
        fill(c, p, cx, cy, rx, ry, 0x66FFFFFF);
    }

    static void line(Canvas c, Paint p, float x1, float y1, float x2, float y2, float w, int col) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(w);
        p.setColor(col);
        c.drawLine(x1, y1, x2, y2, p);
        p.setStyle(Paint.Style.FILL);
        p.setStrokeCap(Paint.Cap.BUTT);
    }

    static void leaf(Canvas c, Paint p, float x1, float y1, float x2, float y2, float w, int col) {
        float mx = (x1 + x2) / 2f, my = (y1 + y2) / 2f;
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy) + 0.001f;
        float nx = -dy / len * w, ny = dx / len * w;
        P.reset();
        P.moveTo(x1, y1);
        P.quadTo(mx + nx, my + ny, x2, y2);
        P.quadTo(mx - nx, my - ny, x1, y1);
        p.setStyle(Paint.Style.FILL);
        p.setColor(col);
        c.drawPath(P, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.4f);
        p.setColor(OUT);
        c.drawPath(P, p);
        p.setStyle(Paint.Style.FILL);
    }

    static void stem(Canvas c, Paint p, float x1, float y1, float cx, float cy, float x2, float y2, float w, int col) {
        P.reset();
        P.moveTo(x1, y1);
        P.quadTo(cx, cy, x2, y2);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(w + 4.5f);
        p.setColor(OUT);
        c.drawPath(P, p);
        p.setStrokeWidth(w);
        p.setColor(col);
        c.drawPath(P, p);
        p.setStyle(Paint.Style.FILL);
        p.setStrokeCap(Paint.Cap.BUTT);
    }

    static void eye(Canvas c, Paint p, float x, float y, float r, float look, float blink) {
        R.set(x - r, y - r * blink, x + r, y + r * blink);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFFFFFFF);
        c.drawOval(R, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        p.setColor(OUT);
        c.drawOval(R, p);
        p.setStyle(Paint.Style.FILL);
        if (blink > 0.4f) {
            fill(c, p, x + look * r * .25f, y, r * .5f, r * .55f * blink, 0xFF16210F);
            fill(c, p, x + look * r * .25f - r * .15f, y - r * .22f, r * .16f, r * .16f, 0xFFFFFFFF);
        }
    }

    static void smile(Canvas c, Paint p, float x, float y, float w, float h, float sw) {
        R.set(x - w, y - h, x + w, y + h);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(sw);
        p.setColor(OUT);
        c.drawArc(R, 25, 130, false, p);
        p.setStyle(Paint.Style.FILL);
        p.setStrokeCap(Paint.Cap.BUTT);
    }

    private static float blink(float t, float phase) {
        float f = ((t + phase) * 0.33f) % 1f;
        return f < 0.045f ? 0.12f : 1f;
    }

    // ---------- plants ----------
    /** s = kích thước ô (px). t = thời gian. shot = giây kể từ lần bắn/hành động gần nhất. */
    static void plant(Canvas c, Paint p, int type, float cx, float cy, float s,
                      float t, float shot, boolean pf, boolean armed) {
        if (drawGardenPlant(c, p, type, cx, cy, s, t, shot, food)) return;

        c.save();
        c.translate(cx, cy);
        float k = s / 100f;
        c.scale(k, k);
        switch (type) {
            case SUN: sunflower(c, p, t, pf); break;
            case PEA: shooter(c, p, t, shot, false, false); break;
            case REP: shooter(c, p, t, shot, true, pf); break;
            case CHOMP: chomper(c, p, t, shot, pf); break;
            case MINE: mine(c, p, t, armed); break;
            case CAB: pult(c, p, t, shot, 0xFF6CC24A, 0xFF4E9B34, A_CAB); break;
            case KER: pult(c, p, t, shot, 0xFFE9CF4A, 0xFF5CAE3C, A_KER); break;
            case MEL: pult(c, p, t, shot, 0xFF3E9B3B, 0xFF2F7D2E, A_MEL); break;
            default: break;
        }
        c.restore();
        p.setStyle(Paint.Style.FILL);
    }

    private static void sunflower(Canvas c, Paint p, float t, boolean pf) {
        float sw = (float) Math.sin(t * 2f) * 2.5f;
        stem(c, p, 0, 50, 3, 28, sw, 8, 8, 0xFF4E9B34);
        leaf(c, p, 1, 38, -30, 28, 8, 0xFF5CB43E);
        leaf(c, p, 1, 42, 30, 34, 8, 0xFF5CB43E);
        float hx = sw, hy = -10;
        float spin = pf ? t * 90f : 0f;
        for (int ring = 0; ring < 2; ring++) {
            int n = 12;
            for (int i = 0; i < n; i++) {
                c.save();
                c.rotate(i * 360f / n + (ring == 0 ? 15f : 0f) + spin, hx, hy);
                oval(c, p, hx, hy - 29 + ring * 2, 7.5f, 13f, ring == 0 ? 0xFFFFA726 : 0xFFFFD43B);
                c.restore();
            }
        }
        oval(c, p, hx, hy, 21, 21, 0xFF8B5222);
        fill(c, p, hx, hy + 3, 17, 16, 0xFF9C6530);
        float b = blink(t, 0f);
        eye(c, p, hx - 8, hy - 4, 5.5f, 0.4f, b);
        eye(c, p, hx + 8, hy - 4, 5.5f, 0.4f, b);
        smile(c, p, hx, hy + 2, 9, 8, 2.6f);
    }

    private static void shooter(Canvas c, Paint p, float t, float shot, boolean rep, boolean gat) {
        float sw = (float) Math.sin(t * 2.2f) * 2.5f;
        float rc = shot < 0.18f ? 1f - shot / 0.18f : 0f;
        int g1 = rep ? 0xFF4FA83A : 0xFF86DB5A;
        int g2 = rep ? 0xFF3A8A2B : 0xFF63BE3F;
        stem(c, p, -2, 50, -6, 28, -5 + sw, 6, 8, 0xFF4E9B34);
        leaf(c, p, -4, 40, -38, 32, 9, 0xFF5CB43E);
        leaf(c, p, -2, 45, 26, 42, 8, 0xFF5CB43E);
        float hx = -4 + sw - rc * 3.5f, hy = -6 + rc * 1.5f;
        if (rep) {
            // nòng thứ hai (bắn đôi)
            oval(c, p, hx + 22 - rc * 3, hy - 15, 13, 8.5f, g2);
            fill(c, p, hx + 33 - rc * 3, hy - 15, 4, 6, 0xFF1B3D10);
            if (gat) oval(c, p, hx + 22 - rc * 3, hy + 14, 13, 8.5f, g2);
        }
        oval(c, p, hx, hy, 26, 24, g1);
        shine(c, p, hx - 9, hy - 13, 10, 6);
        oval(c, p, hx + 22 - rc * 4, hy + 1, 15, 12, g2);
        fill(c, p, hx + 34 - rc * 4, hy + 1, 5 + rc * 2, 9 + rc * 2, 0xFF1B3D10);
        float b = blink(t, 1.3f);
        eye(c, p, hx - 9, hy - 7, 6.5f, 0.6f, b);
        eye(c, p, hx + 6, hy - 8, 8f, 0.7f, b);
        if (rep) {
            line(c, p, hx - 1, hy - 20, hx + 15, hy - 14, 4.5f, OUT); // lông mày giận dữ
            line(c, p, hx - 16, hy - 15, hx - 4, hy - 19, 4f, OUT);
            leaf(c, p, hx - 8, hy - 22, hx - 20, hy - 42, 6, 0xFFD94A3A);
        } else {
            leaf(c, p, hx - 6, hy - 22, hx - 15, hy - 40, 6, 0xFF4E9B34);
        }
        if (gat) { // mũ sắt Gatling
            oval(c, p, hx, hy - 16, 28, 16, 0xFF7C8A72);
            shine(c, p, hx - 8, hy - 22, 10, 4);
            line(c, p, hx - 26, hy - 8, hx + 26, hy - 8, 4f, 0xFF4E5A48);
        }
    }

    private static void chomper(Canvas c, Paint p, float t, float shot, boolean pf) {
        float chew = shot < 3.0f ? 1f : 0f; // đang nhai khi vừa cắn
        float open = chew > 0 ? 0.08f + 0.10f * (float) Math.abs(Math.sin(t * 9f)) : 0.55f + 0.15f * (float) Math.sin(t * 3f);
        if (pf) open = 0.9f;
        float sway = (float) Math.sin(t * 2f) * 3f;
        leaf(c, p, 0, 44, -32, 36, 8, 0xFF5CB43E);
        leaf(c, p, 2, 46, 32, 40, 8, 0xFF5CB43E);
        stem(c, p, 0, 50, -4, 30, sway - 4, 12, 9, 0xFF6BAE45);
        float hx = sway - 2, hy = -8;
        float up = open * 14f;
        // hàm dưới
        oval(c, p, hx + 6, hy + 14 + up * .55f, 27, 14, 0xFF7B3FA6);
        // trong miệng
        fill(c, p, hx + 6, hy + 1, 24, 6 + open * 12f, 0xFF8E1F3A);
        fill(c, p, hx + 8, hy + 8 + up * .2f, 12, 5, 0xFFE05A78);
        // răng dưới
        for (int i = 0; i < 5; i++) {
            float tx = hx - 12 + i * 9f;
            P.reset();
            P.moveTo(tx - 4, hy + 8 + up * .2f);
            P.lineTo(tx, hy + 8 + up * .2f - 7);
            P.lineTo(tx + 4, hy + 8 + up * .2f);
            P.close();
            p.setColor(0xFFFFFFFF);
            c.drawPath(P, p);
        }
        // hàm trên
        oval(c, p, hx + 2, hy - 14 - up * .55f, 29, 22, 0xFF9650C8);
        shine(c, p, hx - 8, hy - 26 - up * .5f, 11, 6);
        for (int i = 0; i < 5; i++) {
            float tx = hx - 14 + i * 9f;
            float by = hy - 3 - up * .15f;
            P.reset();
            P.moveTo(tx - 4, by);
            P.lineTo(tx, by + 8);
            P.lineTo(tx + 4, by);
            P.close();
            p.setColor(0xFFFFFFFF);
            c.drawPath(P, p);
        }
        float b = blink(t, .7f);
        eye(c, p, hx - 8, hy - 20 - up * .5f, 7f, .5f, b);
        eye(c, p, hx + 10, hy - 21 - up * .5f, 7f, .5f, b);
        for (int i = 0; i < 4; i++) { // đốm tím
            fill(c, p, hx - 14 + i * 12f, hy - 31 - up * .5f + (i % 2) * 5f, 3, 3, 0xFFB57BE0);
        }
    }

    private static void mine(Canvas c, Paint p, float t, boolean armed) {
        fill(c, p, 0, 38, 36, 12, 0xFF6B4A2A);
        if (!armed) {
            oval(c, p, 0, 36, 24, 10, 0xFF8A6238);
            stem(c, p, 0, 30, 0, 22, 4, 14, 4, 0xFF4E9B34);
            leaf(c, p, 4, 14, 14, 6, 4, 0xFF5CB43E);
            return;
        }
        float bob = (float) Math.sin(t * 4f) * 1.2f;
        oval(c, p, 0, 22 + bob, 28, 22, 0xFFD3A864);
        shine(c, p, -10, 10 + bob, 9, 5);
        fill(c, p, -13, 28 + bob, 3, 3, 0xFFA97F3C);
        fill(c, p, 12, 31 + bob, 3, 3, 0xFFA97F3C);
        float b = blink(t, 2f);
        eye(c, p, -9, 18 + bob, 6.5f, 0.3f, b);
        eye(c, p, 9, 18 + bob, 6.5f, 0.3f, b);
        smile(c, p, 0, 24 + bob, 7, 5, 2.4f);
        line(c, p, 0, 2 + bob, 0, -16, 3f, 0xFF5E4220);
        boolean on = ((int) (t * 4f)) % 2 == 0;
        oval(c, p, 0, -20, 7, 7, on ? 0xFFFF3B30 : 0xFF8F1C16);
        if (on) fill(c, p, 0, -20, 13, 13, 0x44FF3B30);
    }

    /** Cây ném: Cabbage / Kernel / Melon-pult. */
    private static void pult(Canvas c, Paint p, float t, float shot, int body, int leafCol, int ammo) {
        float sw = (float) Math.sin(t * 2.4f) * 1.6f;
        float launch = shot < 0.30f ? shot / 0.30f : 1f;
        boolean reloaded = shot > 0.55f;
        leaf(c, p, -6, 40, -36, 22, 10, leafCol);
        leaf(c, p, 6, 40, 36, 24, 10, leafCol);
        leaf(c, p, 0, 38, -2, 8, 10, leafCol);
        // cánh tay ném
        float ang = (float) Math.toRadians(shot < 0.30f ? -125f + 95f * launch
                : -125f + (float) Math.sin(t * 2.2f) * 4f * (reloaded ? 1f : 0.2f));
        float px = 8, py = 12;
        float ex = px + (float) Math.cos(ang) * 40f, ey = py + (float) Math.sin(ang) * 40f;
        line(c, p, px, py, ex, ey, 7.5f, OUT);
        line(c, p, px, py, ex, ey, 4.5f, 0xFF9C6B34);
        oval(c, p, ex, ey, 12, 6.5f, 0xFF7A4E22);
        if (reloaded || shot < 0f) ammo(c, p, ammo, ex, ey - 8f, ammo == A_MEL ? 11f : ammo == A_CAB ? 10f : 7f, 0f);
        // thân
        float by = 24 + sw * .4f;
        oval(c, p, 0, by, 30, 23, body);
        if (ammo == A_MEL) {
            for (int i = -2; i <= 2; i++) line(c, p, i * 11f, by - 18, i * 14f, by + 18, 3.5f, 0xFF1F5E22);
        } else if (ammo == A_KER) {
            for (int i = -1; i <= 1; i++) line(c, p, i * 10f, by - 18, i * 12f, by + 16, 2.5f, 0xFFB8A12A);
        } else {
            leaf(c, p, -22, by + 4, 0, by - 20, 6, 0xFF8EDB6A);
            leaf(c, p, 22, by + 4, 0, by - 20, 6, 0xFF7BCC58);
        }
        shine(c, p, -12, by - 13, 9, 5);
        float b = blink(t, .4f);
        eye(c, p, -9, by - 2, 6.5f, 0.4f, b);
        eye(c, p, 9, by - 2, 6.5f, 0.4f, b);
        smile(c, p, 0, by + 3, 9, 7, 2.6f);
    }

    // ---------- đạn ném ----------
    static void ammo(Canvas c, Paint p, int kind, float x, float y, float r, float rot) {
        c.save();
        c.translate(x, y);
        c.rotate(rot);
        switch (kind) {
            case A_CAB:
                oval(c, p, 0, 0, r, r * .92f, 0xFF8EDB6A);
                line(c, p, -r * .6f, -r * .1f, r * .2f, -r * .7f, r * .18f, 0xFF4E9B34);
                line(c, p, -r * .5f, r * .35f, r * .6f, -r * .2f, r * .18f, 0xFF4E9B34);
                line(c, p, 0, r * .8f, r * .7f, r * .2f, r * .18f, 0xFF4E9B34);
                shine(c, p, -r * .35f, -r * .4f, r * .3f, r * .18f);
                break;
            case A_KER:
                oval(c, p, 0, 0, r * .8f, r, 0xFFFFD43B);
                fill(c, p, -r * .2f, -r * .35f, r * .22f, r * .38f, 0x99FFFFFF);
                break;
            case A_BUTTER:
                R.set(-r, -r * .75f, r, r * .75f);
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFFFFEE6B);
                c.drawRoundRect(R, r * .25f, r * .25f, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1.5f, r * .12f));
                p.setColor(0xFFC9A227);
                c.drawRoundRect(R, r * .25f, r * .25f, p);
                p.setStyle(Paint.Style.FILL);
                fill(c, p, -r * .3f, -r * .3f, r * .35f, r * .15f, 0x88FFFFFF);
                break;
            default:
                oval(c, p, 0, 0, r, r * .85f, 0xFF4FB04A);
                for (int i = -2; i <= 2; i++) line(c, p, i * r * .38f, -r * .7f, i * r * .46f, r * .7f, r * .16f, 0xFF1F6B26);
                shine(c, p, -r * .4f, -r * .4f, r * .3f, r * .16f);
                break;
        }
        c.restore();
        p.setStyle(Paint.Style.FILL);
    }

    // ---------- nắng / máy cắt cỏ ----------
    static void sun(Canvas c, Paint p, float x, float y, float r, float t) {
        c.save();
        c.translate(x, y);
        c.rotate(t * 25f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFFFB300);
        for (int i = 0; i < 10; i++) {
            c.save();
            c.rotate(i * 36f);
            P.reset();
            P.moveTo(-r * .16f, -r * .78f);
            P.lineTo(0, -r * 1.22f);
            P.lineTo(r * .16f, -r * .78f);
            P.close();
            c.drawPath(P, p);
            c.restore();
        }
        c.restore();
        fill(c, p, x, y, r * .92f, r * .92f, 0x33FFE066);
        fill(c, p, x, y, r * .78f, r * .78f, 0xFFFFC72A);
        fill(c, p, x, y, r * .62f, r * .62f, 0xFFFFE45C);
        shine(c, p, x - r * .25f, y - r * .3f, r * .25f, r * .14f);
        fill(c, p, x - r * .22f, y - r * .08f, r * .08f, r * .12f, 0xFF5A3A0A);
        fill(c, p, x + r * .22f, y - r * .08f, r * .08f, r * .12f, 0xFF5A3A0A);
        R.set(x - r * .3f, y - r * .2f, x + r * .3f, y + r * .3f);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1.5f, r * .07f));
        p.setColor(0xFF5A3A0A);
        c.drawArc(R, 30, 120, false, p);
        p.setStyle(Paint.Style.FILL);
    }

    static void mower(Canvas c, Paint p, float x, float y, float s, float t, boolean running) {
        c.save();
        c.translate(x, y);
        c.scale(s / 100f, s / 100f);
        line(c, p, 22, -2, 46, -26, 4f, 0xFF555555);
        line(c, p, 46, -26, 54, -26, 4f, 0xFF333333);
        R.set(-30, -12, 30, 18);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFD93A2B);
        c.drawRoundRect(R, 8, 8, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.6f);
        p.setColor(OUT);
        c.drawRoundRect(R, 8, 8, p);
        p.setStyle(Paint.Style.FILL);
        fill(c, p, -8, -4, 12, 4, 0x55FFFFFF);
        fill(c, p, 14, 4, 5, 5, 0xFFFFD43B);
        float wob = running ? (float) Math.sin(t * 40f) * 1.5f : 0f;
        oval(c, p, -18, 20 + wob, 9, 9, 0xFF333333);
        oval(c, p, 18, 20 - wob, 9, 9, 0xFF333333);
        fill(c, p, -18, 20 + wob, 3, 3, 0xFFBBBBBB);
        fill(c, p, 18, 20 - wob, 3, 3, 0xFFBBBBBB);
        c.restore();
    }

    // ---------- vỏ giáp Giga-nut (Plant Food) - cùng khung với ảnh Giga-nut, không lệch frame ----------
    static void armor(Canvas c, Paint p, float x, float y, float s, float pct, float t) {
        float k = .5f + .02f * (float) Math.sin(t * 6f);
        int a = (int) (60 + 110 * Math.max(0f, Math.min(1f, pct)));
        fill(c, p, x, y, s * k, s * k * 1.05f, (a << 24) | 0x5FD3FF);
        R.set(x - s * k, y - s * k * 1.05f, x + s * k, y + s * k * 1.05f);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, s * .03f));
        p.setColor(0xFFBFF0FF);
        c.drawOval(R, p);
        p.setStyle(Paint.Style.FILL);
        fill(c, p, x - s * .2f, y - s * .28f, s * .12f, s * .07f, 0x99FFFFFF);
        if (pct < .6f) line(c, p, x - s * .1f, y - s * .25f, x + s * .05f, y, Math.max(1.5f, s * .015f), 0xCCFFFFFF);
        if (pct < .3f) line(c, p, x + s * .15f, y - s * .2f, x - s * .05f, y + s * .25f, Math.max(1.5f, s * .015f), 0xCCFFFFFF);
    }

    static void grave(Canvas c, Paint p, float x, float y, float s) {
        fill(c, p, x, y + s * .36f, s * .34f, s * .08f, 0x44000000);
        R.set(x - s * .26f, y - s * .2f, x + s * .26f, y + s * .36f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF9AA0A6);
        c.drawRect(R, p);
        R.set(x - s * .26f, y - s * .42f, x + s * .26f, y + s * .02f);
        c.drawOval(R, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, s * .03f));
        p.setColor(0xFF4A4F55);
        R.set(x - s * .26f, y - s * .2f, x + s * .26f, y + s * .36f);
        c.drawRect(R, p);
        p.setStyle(Paint.Style.FILL);
        line(c, p, x, y - s * .22f, x, y + s * .08f, Math.max(2f, s * .04f), 0xFF4A4F55);
        line(c, p, x - s * .1f, y - s * .12f, x + s * .1f, y - s * .12f, Math.max(2f, s * .04f), 0xFF4A4F55);
    }

    static void tile(Canvas c, Paint p, float x, float y, float w, float h, int rgb, float t) {
        int a = (int) (70 + 50 * Math.sin(t * 4f));
        R.set(x - w * .45f, y - h * .45f, x + w * .45f, y + h * .45f);
        p.setStyle(Paint.Style.FILL);
        p.setColor((a << 24) | (rgb & 0xFFFFFF));
        c.drawRoundRect(R, w * .08f, w * .08f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, w * .02f));
        p.setColor(0xCC000000 | (rgb & 0xFFFFFF));
        c.drawRoundRect(R, w * .08f, w * .08f, p);
        p.setStyle(Paint.Style.FILL);
    }

    static void vase(Canvas c, Paint p, float x, float y, float s) {
        fill(c, p, x, y + s * .36f, s * .3f, s * .07f, 0x44000000);
        R.set(x - s * .26f, y - s * .12f, x + s * .26f, y + s * .38f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFB5683A);
        c.drawOval(R, p);
        R.set(x - s * .13f, y - s * .34f, x + s * .13f, y - s * .04f);
        c.drawRect(R, p);
        R.set(x - s * .18f, y - s * .38f, x + s * .18f, y - s * .27f);
        p.setColor(0xFF8A4A24);
        c.drawOval(R, p);
        fill(c, p, x - s * .12f, y + s * .08f, s * .05f, s * .12f, 0x66FFFFFF);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2f, s * .03f));
        p.setColor(0xFF5A2E12);
        R.set(x - s * .26f, y - s * .12f, x + s * .26f, y + s * .38f);
        c.drawOval(R, p);
        p.setStyle(Paint.Style.FILL);
    }

    // Bộ tạo hình riêng cho 4 cây mới và các trạng thái Plant Food.
    private static boolean drawGardenPlant(Canvas c, Paint p, int type, float cx, float cy,
                                           float size, float t, float shot, boolean food) {
        if (type != REP && type != CAB && type != KER && type != MEL) return false;
        c.save();
        c.translate(cx, cy);
        float pulse = 1f + 0.018f * (float)Math.sin(t * 2.6f);
        c.scale(size * 0.01f * pulse, size * 0.01f * pulse);

        if (food) {
            gdOval(c,p,-47,-47,47,47,0x5539F45C);
            gdOval(c,p,-40,-40,40,40,0x4439F45C);
        }

        switch (type) {
            case REP: {
                gdLeaf(c,p,-20,31,22,9,-18,0xFF4B9D35);
                gdLeaf(c,p,19,31,23,9,18,0xFF4B9D35);
                gdLeaf(c,p,0,34,23,8,0,0xFF66B83D);
                gdLine(c,p,0,23,0,0,0xFF347A32,8);
                int count = food ? 4 : 2;
                for (int i=0;i<count;i++) {
                    float x = food ? (i%2==0?-18:12) : (i==0?-14:13);
                    float y = food ? (i<2?-25:-4) : -19;
                    gdOval(c,p,x-17,y-14,x+13,y+14,0xFF70C943);
                    gdOval(c,p,x-17,y-14,x+13,y+14,0xFF285B2B,3, true);
                    gdOval(c,p,x+2,y-9,x+29,y+9,0xFF8BD84D);
                    gdOval(c,p,x+2,y-9,x+29,y+9,0xFF285B2B,3, true);
                    gdOval(c,p,x+22,y-5,x+28,y+5,0xFF24432C);
                    gdOval(c,p,x-7,y-8,x-1,y-1,0xFFFFFFFF);
                    gdOval(c,p,x-5,y-7,x-2,y-2,0xFF172A18);
                }
                if (food) {
                    for (int i=0;i<5;i++) gdOval(c,p,28+(i%2)*5,-30+i*11,34+(i%2)*5,-24+i*11,0xFF9CF15A);
                } else {
                    gdOval(c,p,38,-25,46,-17,0xFF9DE94C);
                    gdOval(c,p,47,-25,55,-17,0xFF9DE94C);
                }
                break;
            }
            case CAB: {
                gdLeaf(c,p,-25,29,25,10,-20,0xFF448F32);
                gdLeaf(c,p,17,31,25,10,20,0xFF4D9C35);
                gdOval(c,p,-12,-2,11,31,0xFF63B23A);
                Path stalk = new Path();
                stalk.moveTo(-2,18); stalk.quadTo(2,-12,19,-28);
                gdPath(c,p,stalk,0xFF3C8732,6);
                gdLeaf(c,p,-12,15,19,8,-25,0xFF69C348);
                gdLeaf(c,p,17,5,17,8,35,0xFF5CAC38);
                gdCabbage(c,p,18,-31,food?19:16);
                gdOval(c,p,-7,5,0,14,0xFFFFFFFF);
                gdOval(c,p,3,5,10,14,0xFFFFFFFF);
                gdOval(c,p,-4,7,-1,12,0xFF1B321A);
                gdOval(c,p,6,7,9,12,0xFF1B321A);
                if (food) {
                    gdCabbage(c,p,38,-3,8);
                    gdCabbage(c,p,-34,-23,7);
                    gdCabbage(c,p,36,-43,7);
                } else {
                    gdCabbage(c,p,42,-42,6);
                }
                break;
            }
            case KER: {
                gdLeaf(c,p,-23,29,25,10,-18,0xFF3F8B30);
                gdLeaf(c,p,20,30,26,10,18,0xFF4A9A36);
                gdLeaf(c,p,-22,7,20,8,-45,0xFF5EB63D);
                gdLeaf(c,p,23,3,21,8,40,0xFF60B83D);
                gdOval(c,p,-20,-39,20,31,0xFFFFC52D);
                gdOval(c,p,-20,-39,20,31,0xFF8D5A19,3,true);
                for (int row=0;row<6;row++) {
                    for (int col=0;col<3;col++) {
                        float x=-12+col*12+(row%2)*2;
                        float y=-29+row*10;
                        gdOval(c,p,x-4,y-4,x+4,y+4,0xFFFFE66A);
                        gdOval(c,p,x-3,y-3,x+1,y+1,0xFFFFF3A0);
                    }
                }
                gdLine(c,p,12,-8,34,-24,0xFF3D8A31,5);
                gdButter(c,p,30,-29,food?15:10);
                if (food) {
                    gdButter(c,p,-33,-29,9);
                    gdButter(c,p,37,7,8);
                    gdButter(c,p,-33,5,8);
                    gdOval(c,p,0,-48,5,-43,0xFFFFF5B0);
                } else {
                    gdOval(c,p,34,-12,42,-4,0xFFFFD34C);
                }
                gdOval(c,p,-9,-13,-3,-5,0xFFFFFFFF);
                gdOval(c,p,4,-13,10,-5,0xFFFFFFFF);
                gdOval(c,p,-6,-11,-3,-6,0xFF202A1A);
                gdOval(c,p,7,-11,10,-6,0xFF202A1A);
                break;
            }
            case MEL: {
                gdLeaf(c,p,-24,31,25,10,-20,0xFF418D32);
                gdLeaf(c,p,22,31,26,10,20,0xFF4A9C35);
                gdOval(c,p,-18,-4,18,32,0xFF66B83D);
                gdOval(c,p,-26,-43,27,3,0xFF2C7735);
                gdOval(c,p,-23,-40,24,0,0xFF64B943);
                gdOval(c,p,-23,-40,24,0,0xFF245C2B,3,true);
                for (int x=-14;x<=14;x+=14) {
                    Path stripe = new Path();
                    stripe.moveTo(x,-38); stripe.quadTo(x-7,-19,x,-2);
                    gdPath(c,p,stripe,0xFF2A7837,4);
                }
                gdLeaf(c,p,-27,8,17,7,-38,0xFF57AC38);
                gdLeaf(c,p,28,7,19,7,38,0xFF57AC38);
                gdOval(c,p,-9,9,-2,17,0xFFFFFFFF);
                gdOval(c,p,5,9,12,17,0xFFFFFFFF);
                gdOval(c,p,-6,11,-3,16,0xFF142B19);
                gdOval(c,p,8,11,11,16,0xFF142B19);
                if (food) {
                    gdMelon(c,p,35,-35,9);
                    gdMelon(c,p,40,0,8);
                    gdMelon(c,p,-36,-8,8);
                } else {
                    gdMelon(c,p,40,-31,6);
                }
                break;
            }
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFFFFFFF);
        p.setAlpha(255);
        c.restore();
        return true;
    }

    private static void gdOval(Canvas c, Paint p, float l, float t, float r, float b, int color) {
        gdOval(c,p,l,t,r,b,color,0,false);
    }
    private static void gdOval(Canvas c, Paint p, float l, float t, float r, float b,
                               int color, float width, boolean stroke) {
        p.setAntiAlias(true);
        p.setStyle(stroke ? Paint.Style.STROKE : Paint.Style.FILL);
        p.setColor(color);
        if (stroke) p.setStrokeWidth(width);
        c.drawOval(l,t,r,b,p);
        p.setStyle(Paint.Style.FILL);
    }
    private static void gdLeaf(Canvas c, Paint p, float x, float y, float rx, float ry,
                               float angle, int color) {
        c.save(); c.rotate(angle,x,y);
        gdOval(c,p,x-rx,y-ry,x+rx,y+ry,color);
        gdOval(c,p,x-rx,y-ry,x+rx,y+ry,0xFF285B2B,2.2f,true);
        c.restore();
    }
    private static void gdLine(Canvas c, Paint p, float x1, float y1, float x2, float y2,
                               int color, float width) {
        p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(width); p.setColor(color); c.drawLine(x1,y1,x2,y2,p);
        p.setStyle(Paint.Style.FILL);
    }
    private static void gdPath(Canvas c, Paint p, Path path, int color, float width) {
        p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND); p.setStrokeWidth(width); p.setColor(color);
        c.drawPath(path,p); p.setStyle(Paint.Style.FILL);
    }
    private static void gdButter(Canvas c, Paint p, float x, float y, float radius) {
        p.setAntiAlias(true); p.setStyle(Paint.Style.FILL); p.setColor(0xFFFFD95B);
        c.drawRoundRect(x-radius,y-radius*.7f,x+radius,y+radius*.7f,radius*.22f,radius*.22f,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(0xFFAB7A22);
        c.drawRoundRect(x-radius,y-radius*.7f,x+radius,y+radius*.7f,radius*.22f,radius*.22f,p);
        p.setStyle(Paint.Style.FILL);
    }
    private static void gdCabbage(Canvas c, Paint p, float x, float y, float radius) {
        gdOval(c,p,x-radius,y-radius,x+radius,y+radius,0xFF61B83C);
        gdOval(c,p,x-radius*.65f,y-radius*.82f,x+radius*.7f,y+radius*.82f,0xFF85D14D);
        gdOval(c,p,x-radius*.25f,y-radius*.62f,x+radius*.45f,y+radius*.4f,0xFFB0E96B);
        gdOval(c,p,x-radius,y-radius,x+radius,y+radius,0xFF2D6930,2,true);
    }
    private static void gdMelon(Canvas c, Paint p, float x, float y, float radius) {
        gdOval(c,p,x-radius,y-radius*.65f,x+radius,y+radius*.65f,0xFF2B7939);
        gdOval(c,p,x-radius*.78f,y-radius*.51f,x+radius*.78f,y+radius*.51f,0xFF5DBB43);
        gdOval(c,p,x-radius,y-radius*.65f,x+radius,y+radius*.65f,0xFF205A30,2,true);
    }

}
