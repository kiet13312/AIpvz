from pathlib import Path
p=Path('app/src/main/java/com/alpvz/gardendefense/MainActivity.java')
s=p.read_text(encoding='utf-8')

def one(old,new,label):
    global s
    n=s.count(old)
    if n!=1:
        raise SystemExit(f'{label}: expected 1, got {n}')
    s=s.replace(old,new,1)

# Shovel must not overlap the BINU card.
one('''                    getWidth() * .465f,
                    getHeight() * .075f,
                    getWidth() * .525f,
                    getHeight() * .15f,''','''                    getWidth() * .82f,
                    getHeight() * .075f,
                    getWidth() * .87f,
                    getHeight() * .15f,''','shovel draw')
one('getWidth() * .495f,\n                    getHeight() * .122f,','getWidth() * .845f,\n                    getHeight() * .122f,','shovel label')
one('inside(x, y, .465f, .075f, .525f, .15f)','inside(x, y, .82f, .075f, .87f, .15f)','shovel touch')

# BINU movement state.
if 'float binuTargetX = 0f;' not in s:
    one('''        boolean binuSound1Finished = false;

        MediaPlayer binuSound1Player;''','''        boolean binuSound1Finished = false;
        float binuX = 0f;
        float binuY = 0f;
        float binuStartX = 0f;
        float binuStartY = 0f;
        float binuTargetX = 0f;
        float binuTargetY = 0f;

        MediaPlayer binuSound1Player;''','BINU state')

# BINU is physically interpolated to the target zombie.
a=s.index('        private void drawBinu(Canvas c) {')
b=s.index('        private void playBinuSound1()',a)
s=s[:a]+'''        private void drawBinu(Canvas c) {
            if (!binuWaiting && !binuJumping) return;
            if (binuRow < 0 || binuCol < 0) return;
            Bitmap b = binuImg;
            if (binuJumping) {
                if (binuFrame == 1) b = binu1Img;
                else if (binuFrame == 2) b = binu2Img;
                else if (binuFrame == 3) b = binu3Img;
                else if (binuFrame >= 4) b = binu4Img;
            }
            if (b == null) return;
            float x = binuJumping ? binuX : left + binuCol * cellW + cellW / 2f;
            float y = binuJumping ? binuY : top + binuRow * cellH + cellH / 2f;
            c.drawBitmap(b, null,
                    new RectF(x - cellW * .37f, y - cellH * .42f,
                            x + cellW * .37f, y + cellH * .42f), p);
        }

'''+s[b:]

a=s.index('        private void updateBinu(long now) {')
b=s.index('        private void updatePeas(float dt) {',a)
s=s[:a]+'''        private void updateBinu(long now) {
            if (binuWaiting && !binuJumping && now >= binuDetectAt && binuSound1Finished) {
                startBinuJump(now);
            }
            if (!binuJumping) return;
            long elapsed = now - binuClock;
            float t = Math.max(0f, Math.min(1f, elapsed / 480f));
            float smooth = t * t * (3f - 2f * t);
            binuX = binuStartX + (binuTargetX - binuStartX) * smooth;
            binuY = binuStartY + (binuTargetY - binuStartY) * smooth;
            binuFrame = Math.max(1, Math.min(4, (int)(elapsed / 120L) + 1));
            if (elapsed >= 480L) {
                binuX = binuTargetX;
                binuY = binuTargetY;
                smashBinu();
                binuJumping = false;
                binuWaiting = false;
                binuFrame = 0;
                binuRow = -1;
                binuCol = -1;
            }
        }

        private void checkBinu(long now) {
            if (binuWaiting || binuJumping) return;
            for (int r = 0; r < ROWS; r++) {
                for (int col = 0; col < COLS; col++) {
                    Plant a = plants[r][col];
                    if (a == null || a.type != BINU) continue;
                    float bx = left + col * cellW + cellW / 2f;
                    Zombie nearest = null;
                    float best = Float.MAX_VALUE;
                    for (Zombie z : zombies) {
                        if (z.hp <= 0 || z.row != r || z.x <= bx) continue;
                        float d = z.x - bx;
                        if (d <= cellW * 1.30f && d < best) {
                            nearest = z;
                            best = d;
                        }
                    }
                    if (nearest != null) {
                        binuRow = r;
                        binuCol = col;
                        binuWaiting = true;
                        binuDetectAt = now + 2000L;
                        binuX = bx;
                        binuY = top + r * cellH + cellH / 2f;
                        return;
                    }
                }
            }
        }

        private void startBinuJump(long now) {
            if (!binuWaiting || binuRow < 0 || binuCol < 0) return;
            float startX = left + binuCol * cellW + cellW / 2f;
            float startY = top + binuRow * cellH + cellH / 2f;
            Zombie target = null;
            float best = Float.MAX_VALUE;
            for (Zombie z : zombies) {
                if (z.hp <= 0 || z.row != binuRow || z.x < startX) continue;
                float d = z.x - startX;
                if (d <= cellW * 1.80f && d < best) {
                    target = z;
                    best = d;
                }
            }
            if (target == null) {
                binuWaiting = false;
                return;
            }
            binuStartX = startX;
            binuStartY = startY;
            binuTargetX = target.x;
            binuTargetY = target.y;
            binuX = startX;
            binuY = startY;
            plants[binuRow][binuCol] = null;
            binuJumping = true;
            binuFrame = 1;
            binuClock = now;
            playBinuSound2();
        }

        private void smashBinu() {
            if (binuRow < 0) return;
            float centerX = binuTargetX;
            float range = cellW * 1.25f;
            for (Zombie z : zombies) {
                if (z.hp <= 0 || z.row != binuRow) continue;
                if (Math.abs(z.x - centerX) <= range) {
                    if (z.boss) z.hp = Math.max(1f, z.hp * .50f);
                    else z.hp = 0;
                }
            }
        }

'''+s[b:]

# Do not duplicate the waiting BINU in drawPlants.
needle='''                    Plant plant = plants[r][col];
                    if (plant == null) continue;'''
if 'if (plant.type == BINU && binuWaiting) continue;' not in s:
    one(needle,needle+'\n                    if (plant.type == BINU && binuWaiting) continue;','BINU duplicate draw')

# Repeater: second shot after 0.5s.
one('fireDelayed(r, col, 30, 250);','fireDelayed(r, col, 30, 500);','Repeater delay')

# Plant Food: one consumption, global -1/3 current HP on non-boss zombies, then original plant effects.
a=s.index('        private void usePlantFood(Plant a) {')
b=s.index('        private void drawOverlay(',a)
s=s[:a]+'''        private void usePlantFood(Plant a) {
            if (a == null || a.type == BINU || a.foodUsed || food <= 0) return;
            long now = System.currentTimeMillis();
            food--;
            a.foodUsed = true;
            playPlantFoodSound();
            for (Zombie z : zombies) {
                if (z.hp > 0 && !z.boss) {
                    z.hp = Math.max(0f, z.hp * (2f / 3f));
                }
            }
            if (a.type == GIGANUT) {
                a.maxHp = 8000;
                a.hp = 8000;
                a.plantFoodUntil = Long.MAX_VALUE;
            } else if (a.type == SUNFLOWER) {
                a.plantFoodUntil = now + 5000L;
                a.last = now - 2000L;
            } else if (a.type == PEASHOOTER || a.type == REPEATER) {
                a.plantFoodUntil = now + 5000L;
                a.last = now - 1000L;
            } else if (a.type == CHOMPER) {
                Zombie z = nearestZombie(a.row, a.col, cellW * 2f);
                if (z != null && !z.boss) z.hp = 0;
            } else if (a.type == MINE) {
                for (Zombie z : zombies) {
                    if (z.row == a.row && !z.boss &&
                            Math.abs(z.x - (left + a.col * cellW + cellW / 2f)) < cellW * 2.2f) {
                        z.hp = Math.max(0f, z.hp - 1800f);
                    }
                }
                a.hp = 0;
            }
        }

'''+s[b:]

# Board Plant Food no extra food--.
s=s.replace('''                        usePlantFood(plants[row][col]);
                        food--;''','''                        usePlantFood(plants[row][col]);''',1)

# Level rows: 1 -> center row, 2-3 -> center 3 rows, 4+ -> all 5 rows.
if 'private int activeRows()' not in s:
    one('        private long spawnDelay() {','''        private int activeRows() {
            if (level == 1) return 1;
            if (level <= 3) return 3;
            return 5;
        }

        private boolean activeRow(int row) {
            int n = activeRows();
            int first = (ROWS - n) / 2;
            return row >= first && row < first + n;
        }

        private long spawnDelay() {''','level row helpers')

one('''            int row = random.nextInt(ROWS);

            boolean boss =''','''            int n = activeRows();
            int first = (ROWS - n) / 2;
            int row = first + random.nextInt(n);

            boolean boss =''','active row spawn')

# Dim locked rows and prohibit planting on them.
board_old='''                    p.setColor(
                            (r + col) % 2 == 0
                                    ? Color.rgb(103, 166, 78)
                                    : Color.rgb(91, 153, 67)
                    );'''
if board_old in s:
    one(board_old,'''                    if (!activeRow(r)) {
                        p.setColor(Color.rgb(72, 110, 62));
                    } else {
                        p.setColor(
                                (r + col) % 2 == 0
                                        ? Color.rgb(103, 166, 78)
                                        : Color.rgb(91, 153, 67)
                        );
                    }''','locked row draw')

mark='''                if (row < 0 || row >= ROWS ||
                        col < 0 || col >= COLS) {
                    return true;
                }

                if (tool == TOOL_SHOVEL) {'''
one(mark,'''                if (row < 0 || row >= ROWS ||
                        col < 0 || col >= COLS) {
                    return true;
                }
                if (!activeRow(row)) return true;

                if (tool == TOOL_SHOVEL) {''','locked row touch')

# Exact 1/3 speed reduction for every zombie class, including boss, while preserving their relative speeds.
one('''                    speed = cellW * .08f;
                    damage = 45;''','''                    speed = cellW * .053333f;
                    damage = 45;''','boss speed')
one('speed = cellW * .12f;','speed = cellW * .08f;','type1 speed')
one('speed = cellW * .30f;','speed = cellW * .20f;','type2 speed')
one('speed = cellW * .19f;','speed = cellW * .126667f;','normal speed')

# Mower visual: body, wheels, handle; keep existing one-use-per-row logic and 17x movement.
a=s.index('        private void drawMowers(Canvas c) {')
b=s.index('        private void drawHp(',a)
s=s[:a]+'''        private void drawMowers(Canvas c) {
            for (Mower m : mowers) {
                float y = top + m.row * cellH + cellH * .72f;
                float bodyL = m.x - cellW * .26f;
                float bodyR = m.x + cellW * .26f;
                float bodyT = y - cellH * .17f;
                float wheelR = Math.max(3f, cellH * .045f);
                p.setColor(m.used ? Color.DKGRAY : Color.rgb(75, 125, 75));
                c.drawRoundRect(bodyL, bodyT, bodyR, y, 8, 8, p);
                p.setColor(Color.DKGRAY);
                c.drawCircle(bodyL + cellW * .10f, y + wheelR, wheelR, p);
                c.drawCircle(bodyR - cellW * .10f, y + wheelR, wheelR, p);
                p.setStrokeWidth(Math.max(3f, cellW * .025f));
                c.drawLine(bodyR - cellW * .04f, bodyT,
                        bodyR + cellW * .18f, bodyT - cellH * .27f, p);
                c.drawLine(bodyR + cellW * .18f, bodyT - cellH * .27f,
                        bodyR + cellW * .28f, bodyT - cellH * .27f, p);
            }
        }

'''+s[b:]

# Reset BINU coordinates when clearing a level.
one('''            binuSound1Finished = false;
            stopBinuSound1();''','''            binuSound1Finished = false;
            binuX = binuY = binuStartX = binuStartY = binuTargetX = binuTargetY = 0f;
            stopBinuSound1();''','BINU reset')

# Safety checks: no removed systems, no duplicated core methods, balanced braces.
low=s.lower()
for banned in ('zen garden','zomvinhhung','conveyor','minigame','finalboss','bossshoottimer'):
    if banned in low:
        raise SystemExit('removed feature found: '+banned)
for sig in ('private void updatePlants(long now) {','private void updateZombies(long now, float dt) {','private void drawBinu(Canvas c) {'):
    if s.count(sig)!=1:
        raise SystemExit('duplicate core method: '+sig)
if s.count('{') != s.count('}'):
    raise SystemExit('brace imbalance')
if 'fireDelayed(r, col, 30, 500);' not in s:
    raise SystemExit('repeater 500ms missing')
if 'z.hp = Math.max(0f, z.hp * (2f / 3f));' not in s:
    raise SystemExit('Plant Food zombie reduction missing')
if 'speed = cellW * .053333f;' not in s or 'speed = cellW * .08f;' not in s or 'speed = cellW * .20f;' not in s or 'speed = cellW * .126667f;' not in s:
    raise SystemExit('zombie speed reduction missing')
if 'if (a.animFrame > 10) {\n                            a.animFrame = 1;' in s:
    raise SystemExit('Peashooter animation still loops')

p.write_text(s,encoding='utf-8')
print('OK',len(s))
