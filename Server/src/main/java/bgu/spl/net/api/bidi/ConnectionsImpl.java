package bgu.spl.net.api.bidi;

import bgu.spl.net.objects.commands.Message;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.NonBlockingConnectionHandler;

import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl implements Connections<Message>{

    private final ConcurrentHashMap<Integer, ConnectionHandler<Message>> connectionPorts  = new ConcurrentHashMap<>();
    private static boolean isDone = false;
    private static ConnectionsImpl instance;
    private static int userId = 0;

    public static ConnectionsImpl getInstance(){
        if (!isDone){
            synchronized (ConnectionsImpl.class){
                if (!isDone){
                    instance = new ConnectionsImpl();
                    isDone = true;
                }
            }
        }
        return instance;
    }

    @Override
    public boolean send(int connectionId, Message msg) {
        if (connectionPorts.get(connectionId) == null)
            return false;
        connectionPorts.get(connectionId).send(msg);
        return true;
    }

    @Override
    public void broadcast(Message msg) {

    }

    @Override
    public void disconnect(int connectionId) {
        connectionPorts.remove(connectionId);
    }

    public int addUser(ConnectionHandler connect){
        int id = userId;
        connectionPorts.put(userId++, connect);
        return id;
    }

}