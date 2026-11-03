package thaumcraft.world.dim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MazeGenerator {
    public int width = 0;
    public int height = 0;
    long seed = 0L;
    Random rand = null;
    public int[][] grid;
    public static final int N = 1;
    public static final int S = 2;
    public static final int E = 4;
    public static final int W = 8;
    public static final int A = 16;
    public static final int B = 32;

    public static int getOPP(int in) {
        switch (in) {
            case 1:
                return 2;
            case 2:
                return 1;
            case 3:
            case 5:
            case 6:
            case 7:
            default:
                return -99;
            case 4:
                return 8;
            case 8:
                return 4;
        }
    }

    public static int getDX(int in) {
        switch (in) {
            case 1:
                return 0;
            case 2:
                return 0;
            case 3:
            case 5:
            case 6:
            case 7:
            default:
                return -99;
            case 4:
                return 1;
            case 8:
                return -1;
        }
    }

    public static int getDY(int in) {
        switch (in) {
            case 1:
                return -1;
            case 2:
                return 1;
            case 3:
            case 5:
            case 6:
            case 7:
            default:
                return -99;
            case 4:
                return 0;
            case 8:
                return 0;
        }
    }

    public MazeGenerator(int width, int height, long seed) {
        this.width = width;
        this.height = height;
        this.seed = seed;
        this.rand = new Random(seed);
        this.grid = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                this.grid[y][x] = 0;
            }
        }
    }

    public boolean generate() {
        int bx = 0;
        int by = 0;
        switch (this.rand.nextInt(4)) {
            case 0:
                bx = 0;
                by = 0;
                break;
            case 1:
                bx = this.width - 2;
                by = this.height - 2;
                break;
            case 2:
                bx = this.width - 2;
                by = 0;
                break;
            case 3:
                bx = 0;
                by = this.height - 2;
        }

        this.grid[by][bx] = 512;
        this.grid[by][bx + 1] = 768;
        this.grid[by + 1][bx] = 1024;
        this.grid[by + 1][bx + 1] = 1280;
        int px = 1 + this.width / 2;
        int py = 1 + this.height / 2;
        this.grid[py][px] = 256;
        ArrayList<MazeGenerator.Loc> cells = new ArrayList<>();
        int l = (this.width + this.height) / 4;

        for (int z = 0; z < l; z++) {
            int w = 1 + this.rand.nextInt(3);
            if (w > 2) {
                l--;
            }

            int qq = this.rand.nextInt(this.width - w);
            int ww = this.rand.nextInt(this.height - w);

            for (int a = qq; a < qq + w; a++) {
                for (int b = ww; b < ww + w; b++) {
                    if (this.grid[b][a] == 0) {
                        this.grid[b][a] = -1;
                    }
                }
            }
        }

        List<Integer> directions = Arrays.asList(1, 2, 4, 8);
        Collections.shuffle(directions, this.rand);
        int xx = px + getDX(directions.get(0));
        int yy = py + getDY(directions.get(0));
        this.grid[py][px] = this.grid[py][px] | directions.get(0);
        if (this.grid[yy][xx] < 0) {
            this.grid[yy][xx] = 0;
        }

        this.grid[yy][xx] = this.grid[yy][xx] | getOPP(directions.get(0));
        cells.add(new MazeGenerator.Loc(xx, yy));
        boolean success = false;

        while (!cells.isEmpty()) {
            int index = this.getNextIndex(cells.size());
            int x = cells.get(index).x;
            int y = cells.get(index).y;
            Collections.shuffle(directions, this.rand);
            boolean carved = false;

            for (int dir : directions) {
                int nx = x + getDX(dir);
                int ny = y + getDY(dir);
                if (0 < nx && nx < this.width - 1 && 0 < ny && ny < this.height - 1) {
                    if (this.grid[ny][nx] == 0) {
                        this.grid[y][x] = this.grid[y][x] | dir;
                        this.grid[ny][nx] = this.grid[ny][nx] | getOPP(dir);
                        cells.add(new MazeGenerator.Loc(nx, ny));
                        carved = true;
                    }

                    if (carved) {
                        success = true;
                        break;
                    }
                }
            }

            if (!carved) {
                cells.remove(index);
            }
        }

        if (!success) {
            return false;
        }

        for (int aa = 0; aa < this.height; aa++) {
            for (int bb = 0; bb < this.width; bb++) {
                if (this.grid[aa][bb] < 0) {
                    this.grid[aa][bb] = 0;
                }
            }
        }

        Collections.shuffle(directions, this.rand);

        for (int dir : directions) {
            int nx = px + getDX(dir);
            int ny = py + getDY(dir);
            if (0 < nx && nx < this.width - 1 && 0 < ny && ny < this.height - 1 && this.grid[ny][nx] > 0 && this.rand.nextBoolean()) {
                this.grid[ny][nx] = this.grid[ny][nx] | getOPP(dir);
                this.grid[py][px] = this.grid[py][px] | dir;
            }
        }

        Collections.shuffle(directions, this.rand);
        boolean connected = false;

        label336:
        for (int ax = 0; ax < 2; ax++) {
            for (int ay = 0; ay < 2; ay++) {
                for (int dir : directions) {
                    int nx = bx + ax + getDX(dir);
                    int ny = by + ay + getDY(dir);
                    if (0 < nx
                        && nx < this.width - 1
                        && 0 < ny
                        && ny < this.height - 1
                        && this.grid[ny][nx] > 0
                        && (new Cell((short)this.grid[ny][nx])).feature == 0) {
                        this.grid[ny][nx] = this.grid[ny][nx] | getOPP(dir);
                        this.grid[by + ay][bx + ax] = this.grid[by + ay][bx + ax] | dir;
                        connected = true;
                        break label336;
                    }
                }
            }
        }

        if (!connected) {
            List<Integer> directions2 = Arrays.asList(1, 2, 4, 8);
            Collections.shuffle(directions2, this.rand);
            success = false;

            label312:
            for (int ax = 0; ax < 2; ax++) {
                for (int ay = 0; ay < 2; ay++) {
                    for (int dir2 : directions2) {
                        int qx = bx + ax + getDX(dir2);
                        int qy = by + ay + getDY(dir2);
                        if (0 < qx && qx < this.width - 1 && 0 < qy && qy < this.height - 1 && this.grid[qy][qx] == 0) {
                            cells.add(new MazeGenerator.Loc(qx, qy));

                            while (!cells.isEmpty()) {
                                int index = this.getNextIndex(cells.size());
                                int x = cells.get(index).x;
                                int y = cells.get(index).y;
                                Collections.shuffle(directions, this.rand);
                                boolean carved = false;

                                for (int dir : directions) {
                                    int nx = x + getDX(dir);
                                    int ny = y + getDY(dir);
                                    if (0 < nx && nx < this.width - 1 && 0 < ny && ny < this.height - 1) {
                                        if (this.grid[ny][nx] == 0) {
                                            this.grid[y][x] = this.grid[y][x] | dir;
                                            this.grid[y][x] = this.grid[y][x] | 25344;
                                            this.grid[ny][nx] = this.grid[ny][nx] | getOPP(dir);
                                            this.grid[ny][nx] = this.grid[ny][nx] | 25344;
                                            cells.add(new MazeGenerator.Loc(nx, ny));
                                            carved = true;
                                        } else if ((new Cell((short)this.grid[ny][nx])).feature == 0) {
                                            this.grid[y][x] = this.grid[y][x] | dir;
                                            this.grid[ny][nx] = this.grid[ny][nx] | getOPP(dir);
                                            this.grid[qy][qx] = this.grid[qy][qx] | getOPP(dir2);
                                            this.grid[by + ay][bx + ax] = this.grid[by + ay][bx + ax] | dir2;
                                            success = true;
                                            break label312;
                                        }

                                        if (carved) {
                                            break;
                                        }
                                    }
                                }

                                if (!carved) {
                                    cells.remove(index);
                                }
                            }
                        }
                    }
                }
            }

            if (!success) {
                return false;
            }
        }

        for (int aa = 0; aa < this.height; aa++) {
            for (int bb = 0; bb < this.width; bb++) {
                Cell c = new Cell((short)this.grid[aa][bb]);
                if (c.feature == 99) {
                    c.feature = 0;
                    this.grid[aa][bb] = c.pack();
                }
            }
        }

        ArrayList<CellLoc> deadEndsloc = new ArrayList<>();

        for (int aa = 0; aa < this.height; aa++) {
            for (int bb = 0; bb < this.width; bb++) {
                Cell c = new Cell((short)this.grid[aa][bb]);
                int exits = (c.north ? 1 : 0) + (c.south ? 1 : 0) + (c.east ? 1 : 0) + (c.west ? 1 : 0);
                if (exits == 1 && c.feature == 0) {
                    deadEndsloc.add(new CellLoc(bb, aa));
                }
            }
        }

        if (deadEndsloc.size() == 0) {
            return false;
        }

        int r = this.rand.nextInt(deadEndsloc.size());
        CellLoc ll = deadEndsloc.get(r);
        Cell c = new Cell((short)this.grid[ll.z()][ll.x()]);
        c.feature = 6;
        this.grid[ll.z()][ll.x()] = c.pack();
        deadEndsloc.remove(r);
        if (deadEndsloc.size() > 0) {
            r = 0;

            while (r < deadEndsloc.size() / 2) {
                int rx = this.rand.nextInt(deadEndsloc.size());
                CellLoc llx = deadEndsloc.get(rx);
                Cell cx = new Cell((short)this.grid[llx.z()][llx.x()]);
                if (cx.feature == 0) {
                    cx.feature = (byte)(7 + this.rand.nextInt(3));
                    this.grid[llx.z()][llx.x()] = cx.pack();
                    deadEndsloc.remove(rx);
                    r++;
                }
            }
        }

        for (int aa = 0; aa < this.height; aa++) {
            for (int bb = 0; bb < this.width; bb++) {
                c = new Cell((short)this.grid[aa][bb]);
                if (c.feature == 0 && (c.north || c.south || c.west || c.east) && this.rand.nextInt(25) == 0) {
                    switch (this.rand.nextInt(8)) {
                        case 0:
                            c.feature = 8;
                            break;
                        case 1:
                            c.feature = 10;
                            break;
                        case 2:
                        case 3:
                            c.feature = 11;
                            break;
                        case 4:
                        case 5:
                            c.feature = 12;
                            break;
                        case 6:
                            c.feature = 13;
                            break;
                        case 7:
                            c.feature = 14;
                    }

                    this.grid[aa][bb] = c.pack();
                }
            }
        }

        return true;
    }

    private int getNextIndex(int ceil) {
        float r = this.rand.nextFloat();
        if (r <= 0.45F) {
            return ceil - 1;
        } else {
            return r <= 0.9F ? this.rand.nextInt(ceil) : 0;
        }
    }

    private class Loc {
        int x;
        int y;

        public Loc(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
