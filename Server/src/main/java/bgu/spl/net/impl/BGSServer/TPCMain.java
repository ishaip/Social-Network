package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.api.MessageEncoderDecoderImpl;
import bgu.spl.net.api.bidi.BidiMessagingProtocolImpl;
import bgu.spl.net.objects.DataBase;
import bgu.spl.net.srv.Server;

public class TPCMain {

    public static void main (String[] args){
        DataBase.getInstance(); //init the database

        Server.threadPerClient(
                8888, //port
                BidiMessagingProtocolImpl::new, //protocol factory
                MessageEncoderDecoderImpl::new //message encoder decoder factory
        ).serve();
    }
}