CFLAGS:=-c -Wall -Weffc++ -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system -lboost_thread -pthread

all: main

#add more dependencies files of the main (practicly every thing else)
main: bin/main.o bin/ConnectionHandler.o bin/Client.o
	@echo 'Building target: Client'
	g++ -o bin/echoExample bin/ConnectionHandler.o bin/main.o bin/Client.o $(LDFLAGS)
	@echo 'Finished linking'

bin/main.o: src/main.cpp
	g++ $(CFLAGS) -o bin/main.o src/main.cpp

bin/ConnectionHandler.o: src/ConnectionHandler.cpp
	g++ $(CFLAGS) -o bin/ConnectionHandler.o src/ConnectionHandler.cpp

bin/Client.o: src/Client.cpp
	$(CC) $(CFLAGS) -o bin/Client.o src/Client.cpp

.PHONY: clean
clean:
	rm -f bin/*