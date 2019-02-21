import java.lang.Long;
import java.util.StringTokenizer;
import java.io.*;
import java.lang.Math;
import java.util.HashMap;

public class C4AI extends AIModule {
    private final static int WIDTH = 7;
    private final static int HEIGHT = 6;
    private final static int H1 = HEIGHT + 1;
    // 0 if p1, 1 is p2
    private int bot;
    private int lastChosen;
    private int rollback;
    private int depth;
    private final static long TOP = 0x1020408102040L;
    // terminate if reached bottom of tree
    private boolean atBottomOfTree;
    private HashMap<String, Integer> boardCache = new HashMap<String, Integer>();
    private HashMap<String, Integer> threatSimulationCache = new HashMap<String, Integer>();
    private long[] curThreatMap = new long[2];
    // ordering for alpha beta pruning
    private final int[] priorityOrdering = new int[]{3, 2, 4, 1, 5, 0, 6};
    private final int weight[] = new int[6];
    // 0: num1
    // 1: num2
    // 2: atari
    // 3: probWin
    // 4: probDraw
    // 5: probLose

    public C4AI(String fileName, int p) {
        bot = p;
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line = br.readLine();
            StringTokenizer token = new StringTokenizer(line, ",");
            for (int i = 0; i < 6; i++) {
                weight[i] = Integer.parseInt(token.nextToken());
            }
            br.close();
        }
        catch (FileNotFoundException e1) {
            System.exit(-1);
        }
        catch (IOException e2) {
            System.exit(-1);
        }
    }

    @Override
    public void getNextMove(final GameState_Opt7x6 state) {
        double iteration = 0;
        boardCache.clear();
        atBottomOfTree = false;
        depth = 5;
        presetMove(state);
        while (!terminate && !atBottomOfTree) {
            minimax(state);
            depth += (int)Math.pow(2, ++iteration);
        }
    }

    // take a movable move to prevent forfait
    private void presetMove(final GameState_Opt7x6 state) {
        for (int i = 0; i < 7; i++) {
            if (state.canMakeMove(priorityOrdering[i])) {
                rollback = chosenMove = priorityOrdering[i];
                return;
            }
        }
    }

    private void minimax(final GameState_Opt7x6 state) {
        int temp;
        int v = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        // beta in initial max node will never gets updated
        int beta = Integer.MAX_VALUE;
        for (int i = 0; i < 7; i++) {
            int col = priorityOrdering[i];
            if (state.canMakeMove(col)) {
                state.makeMove(col);
                temp = value(state, 1, alpha, beta);

                // System.out.print("v[");
                // System.out.print(col);
                // System.out.print("] = ");
                // System.out.println(temp);

                if (terminate) {
                    chosenMove = rollback;
                    return;
                } // rollback move if level not fully examine
                if (temp > v) {
                    alpha = v = temp;
                    lastChosen = chosenMove = col;
                }
                state.unMakeMove();
            }
        }
        rollback = lastChosen;
    }

    private int value(final GameState_Opt7x6 state, final int curDepth, int alpha, int beta) {
        if (state.isGameOver()) {
            if (state.getCoins() >= 42) {
                atBottomOfTree = true;
            }
            if (state.getWinner() - 1 == bot) {
                return 1000 - curDepth;
            } else if (state.getWinner() == 0) {
                return 0;
            } // draw
            return -1000 + curDepth;
        }

        // designated depth
        if (curDepth >= depth) {
            return evalBoard(state, curDepth);
        }

        if (curDepth % 2 == 0) {
            return maxValue(state, curDepth, alpha, beta);
        }
        return minValue(state, curDepth, alpha, beta);
    }

    private int maxValue(final GameState_Opt7x6 state, final int curDepth, int alpha, int beta) {
        int v = Integer.MIN_VALUE;
        int temp;
        for (int i = 0; i < 7; i++) {
            int col = priorityOrdering[i];
            if (state.canMakeMove(col)) {
                state.makeMove(col);
                temp = value(state, curDepth + 1, alpha, beta);
                if (terminate) {
                    break;
                }
                if (temp > v) {
                    v = temp;
                    if (v >= beta) {
                        state.unMakeMove();
                        return v;
                    }
                    if (v > alpha) {
                        alpha = v;
                    }
                }
                state.unMakeMove();
            }
        }
        return v;
    }

    private int minValue(final GameState_Opt7x6 state, final int curDepth, int alpha, int beta) {
        int v = Integer.MAX_VALUE;
        int temp;
        for (int i = 0; i < 7; i++) {
            int col = priorityOrdering[i];
            if (state.canMakeMove(col)) {
                state.makeMove(col);
                temp = value(state, curDepth + 1, alpha, beta);
                if (terminate) {
                    break;
                }
                if (temp < v) {
                    v = temp;
                    if (v <= alpha) {
                        state.unMakeMove();
                        return v;
                    }
                    if (v < beta) {
                        beta = v;
                    }
                }
                state.unMakeMove();
            }
        }
        return v;
    }

    // always evaluate after bot makes a move
    private int evalBoard(GameState_Opt7x6 state, int curDepth) {
        if (boardCache.containsKey(constructKey(state.color))) {
            return boardCache.get(constructKey(state.color));
        }

        int oppo = bot ^ 1;
        int atari = 0, num1 = 0, num2 = 0;
        long oneBit, temp, tempFilter;
        long board = state.color[0] | state.color[1] | TOP;
        long botThreats = 0, oppoThreats = 0;

        if (terminate) {
            return -1000;
        }

        final long f1 = 0x204081L;
        for (int i = 0; i < 6; i++) {
            tempFilter = f1 << i;
            for (int j = 0; j < 4; j++) {
                temp = tempFilter & board;
                if ((tempFilter & state.color[bot]) == temp) {
                    switch (Long.bitCount(temp)) {
                    case 4:
                        boardCache.put(constructKey(state.color), 1000 - curDepth);
                        return 1000 - curDepth;
                    case 3:
                        // check whether is atari
                        oneBit = tempFilter ^ temp;
                        if (oneBit == 1L || ((oneBit >> 1) & board) != 0) {
                            atari++;
                        } else {
                            botThreats |= oneBit;
                        }
                        break;
                    case 2:
                        num2++;
                        break;
                    case 1:
                        num1++;
                        break;
                    case 0:
                        break;
                    default:
                        System.err.println("filter error");
                        System.exit(-1);
                    }
                } // if no opponent pieces blocking

                // check opponent pieces
                if ((tempFilter & state.color[oppo]) == temp) {
                    switch (Long.bitCount(temp)) {
                    case 4:
                        boardCache.put(constructKey(state.color), -1000);
                        return -1000 + curDepth;
                    case 3:
                        // check whether is atari
                        oneBit = tempFilter ^ temp;
                        if (oneBit == 1L || ((oneBit >> 1) & board) != 0) {
                            boardCache.put(constructKey(state.color), -1000 + curDepth);
                            return -1000 + curDepth;
                        } else {
                            oppoThreats |= oneBit;
                        }

                        break;
                    case 2:
                        num2--;
                        break;
                    case 1:
                        num1--;
                        break;
                    case 0:
                        break;
                    default:
                        System.err.println("filter error");
                        System.exit(-1);
                    }
                }
                tempFilter <<= 7;
            }
        }

        final long f2 = 0xfL;
        for (int i = 0; i < 3; i++) {
            tempFilter = f2 << i;
            for (int j = 0; j < 7; j++) {
                temp = tempFilter & board;
                if ((tempFilter & state.color[bot]) == temp) {
                    switch (Long.bitCount(temp)) {
                    case 4:
                        boardCache.put(constructKey(state.color), 1000 - curDepth);
                        return 1000 - curDepth;
                    case 3:
                        atari++;
                        break;
                    case 2:
                        num2++;
                        break;
                    case 1:
                        num1++;
                        break;
                    case 0:
                        break;
                    default:
                        System.err.println("filter error");
                        System.exit(-1);
                    }
                } // if no opponent pieces blocking

                // check opponent pieces
                if ((tempFilter & state.color[oppo]) == temp) {
                    switch (Long.bitCount(temp)) {
                    case 4:
                        boardCache.put(constructKey(state.color), -1000 + curDepth);
                        return -1000 + curDepth;
                    case 3:
                        boardCache.put(constructKey(state.color), -1000 + curDepth);
                        return -1000 + curDepth;
                    case 2:
                        num2--;
                        break;
                    case 1:
                        num1--;
                        break;
                    case 0:
                        break;
                    default:
                        System.err.println("filter error");
                        System.exit(-1);
                    }
                }
                tempFilter <<= 7;
            }
        }

        final long f3 = 0x1010101L;
        for (int i = 0; i < 3; i++) {
            tempFilter = f3 << i;
            for (int j = 0; j < 4; j++) {
                temp = tempFilter & board;
                if ((tempFilter & state.color[bot]) == temp) {
                    switch (Long.bitCount(temp)) {
                    case 4:
                        boardCache.put(constructKey(state.color), 1000 - curDepth);
                        return 1000 - curDepth;
                    case 3:
                        // check whether is atari
                        oneBit = tempFilter ^ temp;
                        if (oneBit == 1L || ((oneBit >> 1) & board) != 0) {
                            atari++;
                        } else {
                            botThreats |= oneBit;
                        }
                        break;
                    case 2:
                        num2++;
                        break;
                    case 1:
                        num1++;
                        break;
                    case 0:
                        break;
                    default:
                        System.err.println("filter error");
                        System.exit(-1);
                    }
                } // if no opponent pieces blocking

                // check opponent pieces
                if ((tempFilter & state.color[oppo]) == temp) {
                    switch (Long.bitCount(temp)) {
                    case 4:
                        boardCache.put(constructKey(state.color), -1000 + curDepth);
                        return -1000 + curDepth;
                    case 3:
                        // check whether is atari
                        oneBit = tempFilter ^ temp;
                        if (oneBit == 1L || ((oneBit >> 1) & board) != 0) {
                            boardCache.put(constructKey(state.color), -1000 + curDepth);
                            return -1000 + curDepth;
                        } else {
                            oppoThreats |= oneBit;
                        }
                        break;
                    case 2:
                        num2--;
                        break;
                    case 1:
                        num1--;
                        break;
                    case 0:
                        break;
                    default:
                        System.err.println("filter error");
                        System.exit(-1);
                    }
                }
                tempFilter <<= 7;
            }
        }

        final long f4 = 0x208208L;
        for (int i = 0; i < 3; i++) {
            tempFilter = f4 << i;
            for (int j = 0; j < 4; j++) {
                temp = tempFilter & board;
                if ((tempFilter & state.color[bot]) == temp) {
                    switch (Long.bitCount(temp)) {
                    case 4:
                        boardCache.put(constructKey(state.color), 1000 - curDepth);
                        return 1000 - curDepth;
                    case 3:
                        // check whether is atari
                        oneBit = tempFilter ^ temp;
                        if (((oneBit >> 1) & board) != 0) {
                            atari++;
                        } else {
                            botThreats |= oneBit;
                        }

                        break;
                    case 2:
                        num2++;
                        break;
                    case 1:
                        num1++;
                        break;
                    case 0:
                        break;
                    default:
                        System.err.println("filter error");
                        System.exit(-1);
                    }
                } // if no opponent pieces blocking

                // check opponent pieces
                if ((tempFilter & state.color[oppo]) == temp) {
                    switch (Long.bitCount(temp)) {
                    case 4:
                        boardCache.put(constructKey(state.color), -1000 + curDepth);
                        return -1000 + curDepth;
                    case 3:
                        // check whether is atari
                        oneBit = tempFilter ^ temp;
                        if ((((oneBit) >> 1) & board) != 0) {
                            boardCache.put(constructKey(state.color), -1000 + curDepth);
                            return -1000 + curDepth;
                        } else {
                            oppoThreats |= oneBit;
                        }
                        break;
                    case 2:
                        num2--;
                        break;
                    case 1:
                        num1--;
                        break;
                    case 0:
                        break;
                    default:
                        System.err.println("filter error");
                        System.exit(-1);
                    }
                }
                tempFilter <<= 7;
            }
        }

        if (atari > 1) {
            boardCache.put(constructKey(state.color), 1000 - curDepth);
            return 1000 - curDepth;
        }

        int v = weight[0] * num1 + weight[1] * num2 + weight[2] * atari;
        if ((botThreats | oppoThreats) != 0) {
            int simWinner;
            if (bot == 0) {
                curThreatMap[0] = botThreats;
                curThreatMap[1] = oppoThreats;
            } else {
                curThreatMap[1] = botThreats;
                curThreatMap[0] = oppoThreats;
            }

            if (threatSimulationCache.containsKey(constructKey(curThreatMap))) {
                simWinner = threatSimulationCache.get(constructKey(curThreatMap));
            } else {
                simWinner = subSimulation();
                threatSimulationCache.put(constructKey(curThreatMap), simWinner);
            }

            if (simWinner == bot) {
                v += weight[3];
            } else if (simWinner == -1) {
                v += weight[4];
            } else {
                v += weight[5];
            }
        }

        boardCache.put(constructKey(state.color), v);
        return v;
    }

    // combine two longs to form a single key for hashmap
    private String constructKey(long tempColor[]) {
        return Long.toHexString(tempColor[0]) + Long.toHexString(tempColor[1]);
    }

    private int bitColNum(final long oneBit) {
        return Long.numberOfTrailingZeros(oneBit) / 7;
    }

    // return -1 if draw, 0 if p1 wins, 1 if p2 wins
    private int subSimulation() {
        long threatMap = curThreatMap[0] | curThreatMap[1];
        int pos1 = Long.numberOfTrailingZeros(threatMap) - 1;
        int firstCol = bitColNum(Long.lowestOneBit(threatMap));
        int secondCol = bitColNum(Long.highestOneBit(threatMap));

        if (firstCol == secondCol) {
            if ((++pos1 % 7) % 2 == 0) {
                return switchResult(singleColSimulation(0, pos1));
            } // p1 to play
            return switchResult(singleColSimulation(1, pos1));
        } // if one threat column

        // two threat columns
        int pos2 = indexOfLowestPieceOfAColumn(threatMap, secondCol) - 1;
        return switchResult(miniminimax(pos1, pos2));
    }

    private int miniminimax(int pos1, int pos2) {
        if ((pos1 % 7 + pos2 % 7) % 2 == 0) {
            return minimaxvalue(pos1, pos2);
        } // p1 to play
        return miniminvalue(pos1, pos2);
    }

    private int switchResult(int r) {
        switch (r) {
        case 1:
            return 0;
        case 0:
            return -1;
        case -1:
            return 1;
        default:
            System.err.println("switch result error");
            return -1;
        }
    }

    // pos about to be taken by player
    private int singleColSimulation(int player, int pos) {
        do {
            if (player == 0) {
                if (((1L << pos) & curThreatMap[0]) != 0) {
                    return 1;
                }
                player = 1;
            } // p1 to play pos
            else {
                if (((1L << pos) & curThreatMap[1]) != 0) {
                    return -1;
                }
                player = 0;
            } // p2 to play pos
        } while (++pos % 7 > 0);
        return 0;
    }

    // key = 0 -> p1 takes pos1
    // key = 1 -> p1 takes pos2
    // key = 2 -> p2 takes pos1
    // key = 3 -> p2 takes pos2
    // minimax p1 as max node, p2 min;
    // return -1 if p1 lose, 0 draw, 1 win;
    private int minivalue(int key, int pos1, int pos2) {
        switch (key) {
        case 0:
            if (((1L << pos1) & curThreatMap[0]) != 0) {
                return 1;
            }
            if (++pos1 % 7 == 0) {
                return singleColSimulation(1, pos2);
            } // end of column
            return miniminvalue(pos1, pos2);
        case 1:
            if (((1L << pos2) & curThreatMap[0]) != 0) {
                return 1;
            } // if connected 4
            if (++pos2 % 7 == 0) {
                return singleColSimulation(1, pos1);
            } // end of column
            return miniminvalue(pos1, pos2);
        case 2:
            if (((1L << pos1) & curThreatMap[1]) != 0) {
                return -1;
            } // if connected 4
            if (++pos1 % 7 == 0) {
                return singleColSimulation(1, pos2);
            } // end of column
            return minimaxvalue(pos1, pos2);
        case 3:
            if (((1L << pos2) & curThreatMap[1]) != 0) {
                return -1;
            } // if connected 4
            if (++pos2 % 7 == 0) {
                return singleColSimulation(1, pos1);
            } // end of column
            return minimaxvalue(pos1, pos2);
        default:
            System.err.println("minivalue error");
            return -1;
        }
    }

    private int miniminvalue(int pos1, int pos2) {
        int v = 1;
        for (int i = 2; i < 4; i++) {
            int temp = minivalue(i, pos1, pos2);
            if (temp < v) {
                v = temp;
            }
        }
        return v;
    }

    private int minimaxvalue(int pos1, int pos2) {
        int v = -1;
        for (int i = 0; i < 2; i++) {
            int temp = minivalue(i, pos1, pos2);
            if (temp > v) {
                v = temp;
            }
        }
        return v;
    }

    private int indexOfLowestPieceOfAColumn(long map, int col) {
        final long columnFilter = 0x3fL;
        return Long.numberOfTrailingZeros((columnFilter << (col * 7)) & map);
    }
}

/*
 * FILTER BANK h: 1 1 1 1;
 *
 * v: 1 1 1 1
 *
 * ld: 1 1 1 1
 *
 * rd: 1 1 1 1
 *
 *
 * general filter: 7*7 bitmask f1 = 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
 * 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 x x x x 0 0 0 <- pattern
 *
 * shift pattern right one column: f2 = 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
 * 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 x x x x 0 0
 *
 * f2 = f1 << 7 eg: f1 = 0x40800000000L 0b 0000001 0000001 0000000 0000000
 * 0000000 0000000 0000000 f2 = 0x810000000 0b 0000000 0000001 0000001 0000000
 * 0000000 0000000 0000000
 *
 * shift pattern up one row: f3 = 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
 * 0 0 0 0 0 0 0 0 0 0 0 0 x x x x 0 0 0 0 0 0 0 0 0 0
 *
 * f3 = f1 << 1 eg: f1 = 0x40800000000L f3 = 0x81000000000 0b 0000010 0000010
 * 0000000 0000000 0000000 0000000 0000000
 */