//
// Created by spl211 on 06/01/2022.
//
#include "../include/ConnectionHandler.h"
#include <thread>
#include "../include/Client.h"

Client::Client(std::string host, short port):
 connectionHandler(host, port),
 terminate(false)
{}

Client::Client(const Client &other):
connectionHandler(other.connectionHandler),
terminate(other.terminate)
{}

Client::~Client() {this->connectionHandler.close();}

void Client::runnableReader() {
    while(!this->terminate && !this->connectionHandler.getIsLoggedOut()){
        std::string answer;
        if (!connectionHandler.getLine(answer)) {
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
        }
        if(!answer.empty()){
            std::cout << answer.substr(0,answer.find(';')) <<"\n";
            if(answer == "ACK 3"){
                waitingForLogoutResponse = false;
                this->terminate = true;
                this->getConnectionHandler().close();
            }
            if(answer == "ERROR 3")
                waitingForLogoutResponse = false;
        }
    }
}

void Client::runnableWriter() {
    while (!this->terminate && !this->connectionHandler.getIsLoggedOut()) {
        while (waitingForLogoutResponse)
            sleep(1);
        if (!this->terminate && !this->connectionHandler.getIsLoggedOut()) {
            const short bufsize(1024);
            char buf[bufsize];
            std::cin.getline(buf, bufsize);
            std::string line(buf);
            if (line == "LOGOUT")
                waitingForLogoutResponse = true;
            if (!connectionHandler.sendLine(line)) {
                std::cout << "Disconnected. Exiting...\n" << std::endl;
                break;
            }
        }
    }
}

bool Client::isTerminated() {return terminate;}

ConnectionHandler &Client::getConnectionHandler() {return connectionHandler;}