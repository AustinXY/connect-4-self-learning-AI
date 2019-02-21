// (c) Ian Davidson, Leo Shamis U.C. Davis 2019


/// Sample AI module that picks the first legal move.
/**
 * This AI chooses the leftmost column. It is meant to illustrate the basics of how to
 * use the AIModule and GameStateModule classes.
 *
 * Since this terminates in under a millisecond, there is no reason to check for
 * the terminate flag.  However, your AI needs to check for the terminate flag.
 *
 * @author Leonid Shamis
 */
public class StupidAI extends AIModule
{
	public void getNextMove(final GameState_Opt7x6 game)
	{
		for(int i = 0; i < game.getWidth(); i++)
			if(game.canMakeMove(i))
			{
				chosenMove = i;
				break;
			}
	}
}
