# connect-4-self-learning-AI

There are four connect four AIs you can play with. StupidAI, RandomAI, MonteCarloAI and C4AI. C4AI is our implementation of connect four AI.<br />
## Usage
* `javac *.java`<br />
Compile.
* `java Main -t 2000 -p1 MonteCarloAI -p2 C4AI -d`<br />
Specify player1 and player2. Any missing player will be filled in with human player. When using C4AI, have to specify a csv file containing the weights of parameters, or use `-d` for default config used in training. `-t` can be used to set AItime, which is the time limit (in ms) for the AI to compute a move. The default of AItime is 1000ms.
* `java Main -train`<br />
Train C4AI.

## Implementation
### General
C4AI implemented MiniMax with Alpha-Beta pruning. Since at early stages of game it's impossible to traverse down to the bottom of game tree, it also implemented iterative deepening DFS with a starting depth of 5. The depth always has to be an odd number since the C4AI's evaluation evaluate board after C4AI has taken a move.
### Evaluation
The evaluation function takes two longs as bitmaps from the board as input, the opponent's pieces and the bot's pieces. It uses four filters to filter both players bitmaps, and calculate, for each players, number of 1 pieces that have potentials to grow into 4, number of 2 pieces that have potentials to grow into 4, number of **ataris**, which is number of slots that can be taken immediately to connect four pieces, and whether either player already has a connected four. Since the evaluation is restricted to happen only after the bot has taken a move, one atari for the opponent signifies a lose. Two ataris for the bot on the other hand signifies a win for the bot.
```
fh = 0 0 0 0 0 0 0 <- TOP row, cannot be taken
     0 0 0 0 0 0 0
     0 0 0 0 0 0 0
     0 0 0 0 0 0 0
     0 0 0 0 0 0 0
     0 0 0 0 0 0 0
     1 1 1 1 0 0 0

fv = 0 0 0 0 0 0 0
     0 0 0 0 0 0 0
     0 0 0 0 0 0 0
     1 0 0 0 0 0 0
     1 0 0 0 0 0 0
     1 0 0 0 0 0 0
     1 0 0 0 0 0 0

fld = 0 0 0 0 0 0 0
      0 0 0 0 0 0 0
      0 0 0 0 0 0 0
      0 0 0 1 0 0 0
      0 0 1 0 0 0 0
      0 1 0 0 0 0 0
      1 0 0 0 0 0 0

frd = 0 0 0 0 0 0 0
      0 0 0 0 0 0 0
      0 0 0 0 0 0 0
      1 0 0 0 0 0 0
      0 1 0 0 0 0 0
      0 0 1 0 0 0 0
      0 0 0 1 0 0 0
```

If no winning or losing has occurred after this point, the evaluation function then takes the threat maps of both players and perform a threat analysis. (Threats are spots that cannot be directly claimed, but will result in a win once taken. More about threats can be found in the Victor Allis's thesis on solving connect four: http://www.informatik.uni-trier.de/~fernau/DSL0607/Masterthesis-Viergewinnt.pdf)

The threat analysis is carried out in a MiniMax approach to simulate until end game to decide whether it is a possible win, lose or draw for the bot. (Since players can only make moves in the threat columns, which can be at most up to two, this simulation can be performed fairly fast, and analyses results are cached in a hashmap for future lookups.)

The evaluation will then use the weight vector to assign utilities for the board state.
```
// num1 = #(bot's 1 piece with potential) - #(opponent's 1 piece with potential)
// num2 = #(bot's 2 piece with potential) - #(opponent's 2 piece with potential)
// atari := whether bot has an immediate winning move
//
// weight is a length 6 int array with each element represent the weight of:
// 0: num1
// 1: num2
// 2: atari
// 3: probWin
// 4: probDraw
// 5: probLose

// if bot probably wins
v = weight[0] * num1 + weight[1] * num2 + weight[2] * atari + weight[3]

// if bot probably draws
v = weight[0] * num1 + weight[1] * num2 + weight[2] * atari + weight[4]

// if bot probably loses
v = weight[0] * num1 + weight[1] * num2 + weight[2] * atari + weight[5]
```
### Training
The performance of an AI, after the model has been constructed, is based almost solely on the quality of parameters. In our case the weight vector. We can get some initial intuitions for the C4AI's weight vector for instance *probWin* should probably have large weight that's close to the utility of actual wins, and *probLose* should probably get a very negative weight. *probDraw* should probably get a value that's close to zero. But all these intuitions aside it's hard to write down or calculate some weights and just say that these are good enough. So we would want to perform training for the AI to learn these weights.

Training is based on very simple ideas. Given some primary configuration, we call this primary C4AI the parent, we randomly mutate the primary weight vector and hand this mutated config to another C4AI, calling this the mutated child. The parent will then play two games with the mutated child, one as player1 and the other as player2. If parent wins more games (or same number of wins) we say that the mutation is not a productive one so the mutated child will then be discarded and another one will be produced by the parent. One the other hand if the mutated child wins more games, the primary config will be replace by the mutation. A `unbeatenRun` counter is updated by the training. It signifies the number of generations the primary configuration has stayed unchanged. It is used to control the relative closeness of the mutation the the primary config.
```
temp = Integer.parseInt(st.nextToken()) +
       ThreadLocalRandom.current().nextInt(-5 * (unbeatenRun+1), 5 * (unbeatenRun+1) + 1);
```
We call two configurations close when the vectors are close. i.e.
dist((x_1,x_2,...,x_n), (y_1,y_2,...,y_n)) = (\sum_{i=0}^{n}(x_i-y_i)^2)^\frac{1}{2}


Everytime the primary config gets updated, we start mutating the config to configs that're closer to it, and gradually increase the distance. We do this to try to make the configuration converge to some local optimal, then try to from there reach some even better configs.

## Copyright
<p style="text-align: left;">
The <b>MonteCarloAI.java</b>, <b>StupidAI.java</b>, <b>RandomAI.java</b>, <b>IOModule.java</b>, <b>GameState_Opt7x6.java</b>, <b>GameController.java</b>, <b>Display.java</b>, <b>TextDisplay.java</b>, <b>AIModule.java</b> are distributed by Ian Davidson at UCDavis.
</p>
