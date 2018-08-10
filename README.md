# MineSolver2

Solve any Minesweeper game on-screen! Linux, Windows, OSX supported. Takes screenshots and makes clicks, uses state of the art algorithms to calculate mines and make good guesses when necessary.

===============================

09/08/2018

===============================

Choosing a language

I thought it would be easy to just choose a language, but it's not. I ended up infatuated with Julia before discovering it's really not free lunch. Here's a jot of ideas so hopefully I can come to a conclusion soon:

Julia pros: Novel for my education | (Claims to be) Fast | Good package management | Good for numeric algorithms 

Julia cons: Immature | Not designed for apps | 1-indexed; prefered technical scripts than developing apps

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
