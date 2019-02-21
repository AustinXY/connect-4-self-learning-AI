import java.util.concurrent.ThreadLocalRandom;
import java.util.StringTokenizer;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.swing.*;

public class Main
{
    public static void main(String[] args)
    {
        int temp, win, lose, draw, unbeatenRun = 0, filenum = 0;
        int numparameters = 6;
        AIModule[] players = new AIModule[2];
        GameController controller;
        int AI_time = 1000;
        GameState_Opt7x6 game;
        IOModule io;
        boolean train = false;

        String filename, path = "/Users/austin/Desktop/c4_self_learning/";
        String parentFilename = "Primaryconfig.csv";
        String mutationFilename = "Mutateconfig.csv";

        try {
            int i = 0;
            while (i < args.length) {
                if (args[i].equalsIgnoreCase("-train")) {
                    train = true;
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
            if (unbeatenRun >= 100) {
                unbeatenRun = 0;
                filename = "archivedconfig" + Integer.toString(++filenum) + ".csv";
                try {
                    Files.copy(new File(path + parentFilename).toPath(),
                        new File(path + filename).toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ioe) {
                    System.err.println("error copy file");
                    System.exit(-1);
                }

                for (int i = 0; i < numparameters; i++) {
                    weight[i] = Integer.toString(ThreadLocalRandom.current().nextInt(-1000, 1000));
                }

                try {
                    outf = new FileWriter(parentFilename);
                    BufferedWriter writer = new BufferedWriter(outf);
                    writer.write(String.join(",", weight));
                    writer.close();
                    outf.close();
                } catch (IOException ee) {
                    System.err.println("file write error");
                    System.exit(-1);
                }
            } // if 100 generations unbeaten

            win = 0;
            lose = 0;
            draw = 0;

            try {
                inf = new FileReader(parentFilename);
                BufferedReader br = new BufferedReader(inf);
                String line = br.readLine();
                StringTokenizer st = new StringTokenizer(line, ",");
                for (int i = 0; i < numparameters; i++) {
                    temp = Integer.parseInt(st.nextToken()) + ThreadLocalRandom.current().nextInt(-5 * (unbeatenRun+1), 5 * (unbeatenRun+1) + 1);
                    if (temp > 1000)
                        temp = 1000;
                    if (temp < -1000)
                        temp = -1000;
                    weight[i] = Integer.toString(temp);
                    br.close();
                }
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

            players[0] = new C4AI(parentFilename, 0);
            players[1] = new C4AI(mutationFilename, 1);
            game = new GameState_Opt7x6();
            io = new TextDisplay();
            controller = new GameController(game, io, players, AI_time);
            controller.play();
            if(game.getWinner() != 0) {
                if (game.getWinner() == 1) {
                    win++;
                } else {
                    lose++;
                }
            } else {
                draw++;
            }

            players[1] = new C4AI(parentFilename, 1);
            players[0] = new C4AI(mutationFilename, 0);
            game = new GameState_Opt7x6();
            io = new TextDisplay();
            controller = new GameController(game, io, players, AI_time);
            controller.play();
            if (game.getWinner() != 0) {
                if (game.getWinner() == 2) {
                    win++;
                } else {
                    lose++;
                }
            } else {
                draw++;
            }

            System.out.println();
            System.out.println("**************************************************");
            System.out.print("win: ");
            System.out.println(win);
            System.out.print("lose: ");
            System.out.println(lose);
            System.out.print("draw: ");
            System.out.println(draw);
            unbeatenRun++;

            if (win < lose) {
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
                } catch (IOException e3) {
                    System.err.println("file write error");
                    System.exit(-1);
                }
            }

            System.out.print("unbeatenRun: ");
            System.out.println(unbeatenRun);
        }
    }
}
