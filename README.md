# MineSolver2

Solve any Minesweeper game on-screen! Linux, Windows, OSX supported. Takes screenshots and makes clicks, uses state of the art algorithms to calculate mines and make good guesses when necessary.
===============================

09/08/2018
===============================

Choosing a language

I thought it would be easy to just choose a language, but it's not. I ended up infatuated with Julia before discovering it's really not free lunch. Here's a jot of ideas so hopefully I can come to a conclusion soon:

Julia pros: Novel for my education | (Claims to be) Fast | Good package management | Good for numeric algorithms 

Julia cons: Immature | Not designed for apps | 1-indexed; preferred technical scripts than developing apps

C++ pros: Fastest | Time tested | Cross platform | Well supported | Compiles into distributable binaries | IO between OS for screen-capture | Good debugging tools | Boss-like feel 

C++ cons: No package manager for SDL or graphics | 

Java pros: Excellent packages | Cross platform | Easy screen capture & mouse w/Robot | Easy cross platform GUI | Good debugging tools

Java cons: Managed, slight performance downgrade | Ugly and verbose

Conclusion:

An app, with GUI and Robot written in Java, and a core algorithm library written in C++ bound to Java using JNI seems like the way to go.

10/08/2018
===============================

I have decided on the goals for this project. They are ordered in terms of personal priority. Choices I make down the road reflect these priorities:

1. Get myself back into software after a year in prison
2. Create a usable and friendly piece of software back by good principals (for my experience)
3. Create an open, cross-platform project that others can contribute to.
4. Create the best minesweeper AI

After spending pretty much 3 entire days just to try to get building to work, I have resigned to using Java for the entire project, with gradle. Here's why:

1. Good cross-platform building everywhere
2. Robot class does *cross-platform* screen-capture and clicking which would be very difficult on any other language
3. Cross-platform native gui exists. It's not good, but it *exists*.
4. Tolerable performance. Basically the only reason I wanted any C++ at all was that raw control over performance for the algorithms. But Java's performance with JIT is OK, considering the cost of:
5. C++ w/JNI is too difficult to build cross-plat. There are no mature build tools that supports C++ and JNI in multiple platforms without getting really awkward. Using sockets to communicate is overkill. 

This project is more about getting back to software engineering and improving my skills. Wrestling with build systems is not the point, and is something I would only do if paid to do. So with all things considered, the simplicity of just doing everything in Java outweighed the benefits and prestige of doing core parts in C++.

11/08/2018
=================================

Took me all day to get JavaFX working and get used to some basic paradigms and FXML. Have a basic screen up.

End of day:
Finished the minesweeper game! I will commit to not touching the game anymore and focus on the two next things for this project:

1. AI
2. Screen capture

But AI is first to be done.

12/18/2018
==================================

Made a basic AI + some interfaces because Java.

I have observed a big game and realised:
In most practical cases (ie. not pathologically contrived cases) blocks of *flags* are common enough that segregation is very viable. Coupled with a straight up frontier DP and this may be a very fast algorithm. However, the graph scanning algorithm may not need to be so sophisticated, since segregation puts an exponential limit to how large groups can be.

Will be making segregation algorithm, followed by backtracking. Then, the graph + frontier DP + benchmarks to see if it makes any positive impact.

Finally, will be making heuristics or even NN's to decide on choosing uncertain squares.

13/18/2018
=================================

Fixed annoying backtracking bug, and added AI debug display, which looks kind of cool. For today, at least accomplish the following:

1. Fix bug where everything disappears at 150x150 board.
2. Clean code - particularly game app state management, add unit tests for that, and make core backtracking algorithm readable. Add "neighbours" function to MineLocation.

Hopefully accomplish:

3. Next stage in AI: Auto-segregation
4. proper combinatorics for edges
5. smarter DFS of sweeps
6. and benchmarking.

Later will do:

Much better heuristics or even NN for guessing boxes in uncertain situations.
Screen grab and auto click.

