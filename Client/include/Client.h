//
// Created by spl211 on 06/01/2022.
//

#ifndef SPL_3_C_CLIENT_H
#define SPL_3_C_CLIENT_H

#endif //SPL_3_C_CLIENT_H
#include "ConnectionHandler.h"
#include <thread>

class Client{
private:
    ConnectionHandler connectionHandler;
    bool terminate;
    bool waitingForLogoutResponse;
public:
    Client(std::string host, short port);
    Client(const Client & other);
    ~Client();
    void runnableWriter();
    void runnableReader();
    ConnectionHandler & getConnectionHandler();
    bool isTerminated();
    static void shortToBytes(short num, char* bytesArr);
    static short bytesToShort(const char* bytesArr);
};

