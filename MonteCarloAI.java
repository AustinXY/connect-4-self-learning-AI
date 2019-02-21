import java.util.Random;

public class MonteCarloAI extends AIModule {
    private final Random r = new Random(System.currentTimeMillis());
    private int[] moves;

    @Override
    public void getNextMove(final GameState_Opt7x6 state) {
        // Set up the legal moves buffer. We use only one buffer repeatedly to
        // avoid needless memory allocations.
        moves = new int[state.getWidth()];

        // Default to choosing the first column (should be fixed after a few rounds)
        chosenMove = 0;

        // Cache our index.
        final int ourPlayer = state.getActivePlayer();

        // Create value array and set all illegal moves to minimum value.
        // This will be filled in using results from random games:
        // +1 point for each win
        // +0 point for each draw
        // -1 point for each loss.
        // We also initialize all illegal moves to -Integer.MAX_VALUE. We could also
        // have used Integer.MIN_VALUE, but this is a "weird number" because
        // -Integer.MIN_VALUE == Integer.MIN_VALUE.
        int[] values = new int[state.getWidth()];
        for (int i = 0; i < values.length; ++i)
            if (!state.canMakeMove(i))
                values[i] = -Integer.MAX_VALUE;

        // Start simulating games! Continue until told to stop.
        while (!terminate) {
            final int move = getMove(state);
            state.makeMove(move);
            updateGuess(ourPlayer, playRandomGame(state), values, move);
            state.unMakeMove();
        }
    }

    private int getMove(final GameState_Opt7x6 state) {
        // Fill in what moves are legal.
        int numLegalMoves = 0;
        for (int i = 0; i < state.getWidth(); ++i)
            if (state.canMakeMove(i))
                moves[numLegalMoves++] = i;
        // Pick one randomly.
        final int n = r.nextInt(numLegalMoves);
        return moves[n];
    }

    // Given the result of the last game, update our chosen move.
    private void updateGuess(final int ourPlayer, final int result, int[] values, int move) {
        // On a draw, we can skip making changes.
        if (result == 0)
            return;

        // Update the expected value of this move depending on whether we win or lose.
        values[move] += (result == ourPlayer ? 1 : -1);

        // Update the move to be the best known move. This is necessary since we need
        // to have the best move available at all times because we run forever.
        for (int i = 0; i < values.length; ++i)
            if (values[i] > values[chosenMove])
                chosenMove = i;
    }

    private int playRandomGame(final GameState_Opt7x6 state) {
        // Duplicate the state to prevent changes from propagating.
        final GameState_Opt7x6 game = state.copy();
        while (!game.isGameOver())
            game.makeMove(getMove(game));

        // It's over! Return who won.
        return game.getWinner();
    }
}