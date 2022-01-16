#include <iostream>
#include <stdlib.h>
#include "../include/Client.h"

int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);

    Client client(host, port);
    if (!client.getConnectionHandler().connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }

    std::thread writingThread(&Client::runnableWriter,&client);
    std::thread readingThread(&Client::runnableReader,&client);
    //in c+ thread start running as soon as he is created

    readingThread.join();
    writingThread.join();
     if(client.isTerminated())
         client.getConnectionHandler().close();

    return 0;
}