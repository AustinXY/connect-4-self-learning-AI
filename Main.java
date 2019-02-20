import java.util.concurrent.ThreadLocalRandom;
import java.util.StringTokenizer;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Main
{
	private final static int numparameters = 6;
	private final static int width = 7;
	private final static int height = 6;

	public static void main(String[] args)
	{
		int temp, parentWin, parentLose, unbeatenRun = 0, filenum = 0;
		AIModule[] players = new AIModule[2];
		String filename, path = "/Users/austin/Desktop/c4_self_learning/";
		String parentFilename = "Primaryconfig.csv";
		String mutationFilename = "Mutateconfig.csv";
		FileReader inf;
		FileWriter outf;
		int AI_time = 1000;
		String weight[] = new String[6];
		GameState_Opt7x6 game;
		IOModule io;

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
			}

			System.out.println(unbeatenRun);
			parentWin = 0;
			parentLose = 0;

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
			}

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
			GameController controller = new GameController(game, io, players, AI_time);
			controller.play();
			if(game.getWinner() != 0) {
				if (game.getWinner() == 1) {
					parentWin++;
				} else {
					parentLose++;
				}
			}

			players[1] = new C4AI(parentFilename, 1);
			players[0] = new C4AI(mutationFilename, 0);
			game = new GameState_Opt7x6();
			io = new TextDisplay();
			controller = new GameController(game, io, players, AI_time);
			controller.play();
			if (game.getWinner() != 0) {
				if (game.getWinner() == 2) {
					parentWin++;
				} else {
					parentLose++;
				}
			}

			System.out.println();
			System.out.println("***********************************************");
			System.out.print("parentWins:");
			System.out.println(parentWin);
			System.out.print("parentLoses:");
			System.out.println(parentLose);
			unbeatenRun++;

			if (parentWin < parentLose) {
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
		}
	}
}