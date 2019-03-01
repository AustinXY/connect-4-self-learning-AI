import java.util.concurrent.ThreadLocalRandom;
import java.util.StringTokenizer;
import java.io.*;
import javax.swing.*;

public class Main
{
    public static void main(String[] args)
    {
        final int numparameters = 6;
        int temp, unbeatenRun = 0;
        AIModule[] players = new AIModule[2];
        GameController controller;
        int AI_time = 1000;
        GameState_Opt7x6 game;
        IOModule io;
        boolean train = false;
        boolean keep; // whether to keep parent config
        boolean draw;
        boolean badConfig = false;
        boolean test = false;
        boolean ptune = true; // tune single weight
        int pindex = 0;
        int tempw = -1000;

        String parentFilename = "Primaryconfig.csv";
        String mutationFilename = "Mutateconfig.csv";

        try {
            int i = 0;
            while (i < args.length) {
                if (args[i].equalsIgnoreCase("-train")) {
                    train = true;
                    break;
                } else if (args[i].equalsIgnoreCase("-train-test")) {
                    train = true;
                    test = true;
                    break;
                }

                if (args[i].equalsIgnoreCase("-p1")) {
                    if (args[i + 1].equals("C4AI")) {
                        if (args[i + 2].equalsIgnoreCase("-d"))
                            players[0] = new C4AI(parentFilename, 0);
                        else
                            players[0] = new C4AI(args[i + 2], 0);
                        i++;
                    } else {
                        players[0] = (AIModule) Class.forName(args[i + 1]).newInstance();
                    }
                } else if (args[i].equalsIgnoreCase("-p2")) {
                    if (args[i + 1].equals("C4AI")) {
                        if (args[i + 2].equalsIgnoreCase("-d"))
                            players[1] = new C4AI(parentFilename, 1);
                        else
                            players[1] = new C4AI(args[i + 2], 1);
                        i++;
                    } else {
                        players[1] = (AIModule) Class.forName(args[i + 1]).newInstance();
                    }
                } else if (args[i].equalsIgnoreCase("-t")) {
                    AI_time = Integer.parseInt(args[i + 1]);
                    if (AI_time <= 0)
                        throw new IllegalArgumentException("AI think time must be positive");
                } else {
                    throw new IllegalArgumentException();
                }
                i += 2;
            }
        } catch (ClassNotFoundException cnf) {
            System.err.println("Player Not Found: " + cnf.getMessage());
            System.exit(1);
        } catch (IndexOutOfBoundsException ioob) {
            System.err.println("Invalid Arguments");
            System.exit(2);
        } catch (NumberFormatException e) {
            System.err.println("Invalid Integer: " + e.getMessage());
            System.exit(3);
        } catch (IllegalArgumentException ia) {
            System.err.println("Invalid Arguments: " + ia.getMessage());
            System.exit(4);
        } catch (InstantiationException e) {
            System.err.println("InstantiationException Error");
            System.exit(5);
        } catch (IllegalAccessException il) {
            System.err.println("IllegalAccessException: " + il.getMessage());
            System.exit(4);
        }

        if (!train) {
            game = new GameState_Opt7x6();
            final Display display = new Display();
            io = display;
            final JFrame frame = new JFrame("Connect Four");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(display);
            frame.pack();
            frame.setVisible(true);

            // Turn on the turn based system
            controller = new GameController(game, io, players, AI_time);
            controller.play();
            // Print out the results of the match
            if (game.getWinner() == 0)
                System.out.println("Draw Game");
            else
                System.out.println("Player " + game.getWinner() + " won");

            return;
        }

        FileReader inf;
        FileWriter outf;
        String weight[] = new String[6];
        while (true) {
            if (test) {
                System.out.println("testing against MonteCarloAI");
                for (int i = 0; i < 7; i++) {
                    System.out.println("**************************************************");
                    System.out.println("game " + Integer.toString(i) + " :");
                    players[0] = new MonteCarloAI();
                    players[1] = new C4AI(parentFilename, 1);
                    game = new GameState_Opt7x6();
                    io = new TextDisplay();
                    controller = new GameController(game, io, players, AI_time);
                    controller.play();
                    if (game.getWinner() == 1) {
                        System.out.println("test failed...");
                        badConfig = true;
                        break;
                    }
                }
                System.out.println("test passed...");
                test = false;
            }

            if (unbeatenRun >= 50) {
                ptune = true;
                unbeatenRun = 0;
            } // if 60 gen unbeaten

            try {
                inf = new FileReader(parentFilename);
                BufferedReader br = new BufferedReader(inf);
                String line = br.readLine();
                StringTokenizer st = new StringTokenizer(line, ",");
                if (ptune) {
                    System.out.println("tuning weight #" + Integer.toString(pindex));
                    for (int i = 0; i < numparameters; i++) {
                        if (i == pindex) {
                            temp = tempw;
                        } else {
                            temp = Integer.parseInt(st.nextToken());
                        }

                        weight[i] = Integer.toString(temp);
                    }
                    tempw += (int)Math.pow(8, (double)unbeatenRun);

                    if (unbeatenRun >= 3) {
                        unbeatenRun = 0;
                    }

                    if (tempw >= 1000) {
                        tempw = -1000;
                        if (++pindex >= numparameters) {
                            pindex = 0;
                            ptune = false;
                            unbeatenRun = 0;
                        }
                    }
                } else {
                    for (int i = 0; i < numparameters; i++) {
                        if (badConfig) {
                            temp = Integer.parseInt(st.nextToken()) + ThreadLocalRandom.current().nextInt(-1000, 1001);
                        } else {
                            temp = Integer.parseInt(st.nextToken()) + ThreadLocalRandom.current()
                                    .nextInt(-10 * (unbeatenRun + 1), 10 * (unbeatenRun + 1) + 1);
                        }
                        if (temp > 1000) {
                            temp = 1000;
                        }
                        if (temp < -1000) {
                            temp = -1000;
                        }
                        weight[i] = Integer.toString(temp);
                    }
                }
                br.close();
            } catch (FileNotFoundException e1) {
                System.err.println("file read error");
                System.exit(-1);
            } catch (IOException e2) {
                System.err.println("file read error");
                System.exit(-1);
            } // generate mutation

            try {
                outf = new FileWriter(mutationFilename);
                BufferedWriter writer = new BufferedWriter(outf);
                writer.write(String.join(",", weight));
                writer.close();
            } catch (IOException e3) {
                System.err.println("file write error");
                System.exit(-1);
            }

            keep = false;
            draw = false;
            for (int i = 0; i < 2; i++) {
                players[i] = new C4AI(parentFilename, i);
                players[i^1] = new C4AI(mutationFilename, i^1);
                game = new GameState_Opt7x6();
                io = new TextDisplay();
                controller = new GameController(game, io, players, AI_time);
                controller.play();
                if (game.getWinner() == i+1) {
                    keep = true;
                    break;
                } else if (game.getWinner() == 0 && i == 0) {
                    draw = true;
                } else if (draw == true && game.getWinner() == 0) {
                    keep = true;
                }
            }

            System.out.println();
            System.out.println("**************************************************");
            if (!keep) {
                unbeatenRun = 0;
                try {
                    inf = new FileReader(mutationFilename);
                    BufferedReader br = new BufferedReader(inf);
                    String line = br.readLine();
                    StringTokenizer st = new StringTokenizer(line, ",");
                    for (int i = 0; i < numparameters; i++) {
                        weight[i] = Integer.toString(Integer.parseInt(st.nextToken()));
                    }
                } catch (FileNotFoundException e1) {
                    System.err.println("file read error");
                    System.exit(-1);
                } catch (IOException e2) {
                    System.err.println("file read error");
                    System.exit(-1);
                }

                try {
                    outf = new FileWriter(parentFilename);
                    BufferedWriter writer = new BufferedWriter(outf);
                    writer.write(String.join(",", weight));
                    writer.close();
                    outf.close();
                    System.out.println("Switched...");
                } catch (IOException e3) {
                    System.err.println("file write error");
                    System.exit(-1);
                }
            } else {
                unbeatenRun++;
                System.out.println("Keeping...");
                System.out.print("Unbeaten run: ");
                System.out.println(unbeatenRun);
            }

            if (badConfig && unbeatenRun >= 13) {
                unbeatenRun = 0;
                badConfig = false;
                test = true;
            }
        }
    }
}
