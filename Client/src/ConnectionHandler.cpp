#include <ConnectionHandler.h>
#include "../include/ConnectionHandler.h"

using boost::asio::ip::tcp;

using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;
 
ConnectionHandler::ConnectionHandler(string host, short port):
host_(host), port_(port), io_service_(), socket_(io_service_),isLoggedOut(false){}
    
ConnectionHandler::~ConnectionHandler() {
    close();
}
 
bool ConnectionHandler::connect() {
    std::cout << "Starting connect to "
        << host_ << ":" << port_ << std::endl;
    try {
		tcp::endpoint endpoint(boost::asio::ip::address::from_string(host_), port_); // the server endpoint
        boost::system::error_code error;
		socket_.connect(endpoint, error);
		if (error)
			throw boost::system::system_error(error);
    }
    catch (std::exception& e) {
        std::cerr << "Connection failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}
 
bool ConnectionHandler::getBytes(char bytes[], unsigned int bytesToRead) {
    size_t tmp = 0;
	boost::system::error_code error;
    try {
        while (!error && bytesToRead > tmp ) {
			tmp += socket_.read_some(boost::asio::buffer(bytes+tmp, bytesToRead-tmp), error);			
        }
		if(error)
			throw boost::system::system_error(error);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendBytes(const char bytes[], int bytesToWrite) {
    int tmp = 0;
	boost::system::error_code error;
    try {
        while (!error && bytesToWrite > tmp ) {
			tmp += socket_.write_some(boost::asio::buffer(bytes + tmp, bytesToWrite - tmp), error);
        }
		if(error)
			throw boost::system::system_error(error);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}
 
bool ConnectionHandler::getLine(std::string& line) {
    std::string bytebuffer;
    if(!getFrameAscii(bytebuffer,';'))
        return false;
    char *byteArr[2];
    byteArr[0] = &bytebuffer[0];
    byteArr[1] = &bytebuffer[1];
    short opcode = bytesToShort(*byteArr);
    decodeWithOpcode(line,bytebuffer,opcode);
    return true;
}

void ConnectionHandler::decodeWithOpcode(string &frame, std::string &bytebuffer, short opcode) {
     if (opcode == 9) {
         frame.append("NOTIFICATION");
         char type = bytebuffer[2];
         if (type == 0)
             frame.append(" PM ");
         else
             frame.append(" Public ");
         int end = bytebuffer.find('\0', 3);
         frame.append(bytebuffer.substr(3, end - 3));
         frame.append(" ");
         frame.append(bytebuffer.substr(end + 1, bytebuffer.size() - 1));
     }
     else if(opcode == 10) {
         frame.append("ACK ");
         char *byteArr[2];
         byteArr[0] = &bytebuffer[2];
         byteArr[1] = &bytebuffer[3];
         short messageOpcode = bytesToShort(*byteArr);
         std::stringstream ss;
         ss<<messageOpcode;
         frame.append(ss.str());
         if (messageOpcode == 4) {
             frame.append(" ");
             int index = bytebuffer.find(';');
             frame.append(bytebuffer.substr(4, index-1));
         }
         if (messageOpcode == 7 || messageOpcode == 8) {
             for (int i = 0; i < 4; i++) {
                 std::stringstream s1;
                 frame.append(" ");
                 byteArr[0] = &bytebuffer[(i * 2) + 4];
                 byteArr[1] = &bytebuffer[(i * 2) + 5];
                 short s = bytesToShort(*byteArr);
                 s1<<s;
                 frame.append(s1.str());
             }
         }
     }
     else if (opcode == 11) {
         frame.append("ERROR ");
         char *byteArr[2];
         byteArr[0] = &bytebuffer[2];
         byteArr[1] = &bytebuffer[3];
         std::stringstream ss;
         ss<<bytesToShort(*byteArr);
         frame.append(ss.str());
     }
}

short ConnectionHandler::getShort(){
    char *bytesArr[2];
    try {
        getBytes(bytesArr[0], 1);
        getBytes(bytesArr[1], 1);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
    }
    return bytesToShort(*bytesArr);
}
 
bool ConnectionHandler::getFrameAscii(std::string& frame, char delimiter) {
    char ch;
    // Stop when we encounter the null character. 
    // Notice that the null character is not appended to the frame string.
    try {
		do{
			getBytes(&ch, 1);
            frame.append(1, ch);
        }while (delimiter != ch);
    } catch (std::exception& e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendLine(std::string& line) {
    int end = 0;

    if ( line.substr(0, 8).compare("REGISTER") == 0 ){
        sendShort( (short) 1); //send opcode

        line = line.substr(9);
        end = line.find(' ');
        if ( !sendFrameAscii(line.substr(0, end), '\0') ) //send username
            return false;

        line = line.substr(end + 1);
        end = line.find(' ');
        sendFrameAscii(line.substr(0, end), '\0'); //send password

        line = line.substr(end + 1);
        sendFrameAscii(line, '\0'); //send birthday

        sendBytes(new char[1]{';'}, 1);
    }
    else if ( line.substr(0, 5).compare("LOGIN") == 0 ){
        sendShort(2); //send opcode

        line = line.substr(6);
        end = line.find(' ');
        sendFrameAscii(line.substr(0, end), '\0'); //send username

        line = line.substr(end + 1);
        end = line.find(' ');
        sendFrameAscii(line.substr(0, end), '\0'); //send password

        if ( line.at(end + 1) == '0' )
            sendBytes(new char[1]{0}, 1);
        else
            sendBytes(new char[1]{1}, 1);

        sendBytes(new char[1]{';'}, 1);
    }
    else if ( line.substr(0, 6).compare("LOGOUT") == 0 ){
        sendShort(3);
        sendBytes(new char[1]{';'}, 1);
    }
    else if ( line.substr(0, 6).compare("FOLLOW") == 0 ){
        sendShort(4); //send opcode

        //send the follow/unfollow byte
        if ( line.at(7) == '0' )
            sendBytes(new char[1]{0}, 1);
        else
            sendBytes(new char[1]{1}, 1);

        sendFrameAscii(line.substr(9), '\0'); //send the username
        sendBytes(new char[1]{';'}, 1);
    }
    else if ( line.substr(0, 4).compare("POST") == 0 ){
        sendShort(5); //send the opcode

        line = line.substr(5);
        sendFrameAscii(line, '\0'); //send the content
        sendBytes(new char[1]{';'}, 1);
    }
    else if ( line.substr(0, 2).compare("PM") == 0 ){
        sendShort(6); //send the opcode

        line = line.substr(3);
        end = line.find(' ');
        sendFrameAscii(line.substr(0, end), '\0'); //send the username

        line = line.substr(end + 1);
        sendFrameAscii(line, '\0'); //send the content

        sendBytes(new char[1]{';'}, 1);
    }
    else if ( line.substr(0, 7).compare("LOGSTAT") == 0 ){
        sendShort(7); //send the opcode
        sendBytes(new char[1]{';'}, 1);
    }
    else if ( line.substr(0, 4).compare("STAT") == 0 ){
        sendShort(8); //send the opcode

        line = line.substr(5);
        end = line.find(' ');
        sendFrameAscii(line.substr(0, end), '\0'); //send the list of the usernames
        sendBytes(new char[1]{';'}, 1);
    }
    else { //BLOCK
        sendShort(12); //send the opcode

        line = line.substr(6);
        end = line.find(' ');
        sendFrameAscii(line.substr(0, end), '\0'); //send the username
        sendBytes(new char[1]{';'}, 1);
    }
    return true;
} //sendFrameAscii(line, '\n');

//short sender
bool ConnectionHandler::sendShort(const short num){
    char *bytesArr;
    shortToBytes(num, bytesArr);
    return sendBytes(bytesArr,2);
}


//string sender
bool ConnectionHandler::sendFrameAscii(const std::string& frame, char delimiter) {
	bool result=sendBytes(frame.c_str(),frame.length());
	if(!result) return false;
	return sendBytes(&delimiter,1);
}
 
// Close down the connection properly.
void ConnectionHandler::close() {
    try{
        socket_.close();
    } catch (...) {
        std::cout << "closing failed: connection already closed" << std::endl;
    }
}

ConnectionHandler::ConnectionHandler(const ConnectionHandler &handler):
host_(handler.host_),
port_(handler.port_),
io_service_(),
socket_(io_service_)
{}

bool ConnectionHandler::getIsLoggedOut() {
    return isLoggedOut;
}

void ConnectionHandler::shortToBytes(short num, char *bytesArr){
    bytesArr[0] = ((num >> 8) & 0xFF);
    bytesArr[1] = (num & 0xFF);
}

short ConnectionHandler::bytesToShort(const char *bytesArr) {
    short result = (short)((bytesArr[0] & 0xff) << 8);
    result += (short)(bytesArr[1] & 0xff);
    return result;
}