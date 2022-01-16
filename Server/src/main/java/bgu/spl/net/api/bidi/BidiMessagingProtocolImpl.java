package bgu.spl.net.api.bidi;

import bgu.spl.net.objects.DataBase;
import bgu.spl.net.objects.Student;
import bgu.spl.net.objects.commands.Message;

import java.sql.SQLSyntaxErrorException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;

public class BidiMessagingProtocolImpl implements BidiMessagingProtocol<Message> {

    private boolean shouldTerminate = false;
    private Student student = null;
    private DataBase db = DataBase.getInstance();
    private int connectionId;

    public BidiMessagingProtocolImpl(){
    }

    public void setStudent (Student st){
        this.student = st;
        st.setId(connectionId);
        st.login();
    }

    @Override
    public void start(int connectionId, Connections<Message> connections) {
        this.connectionId = connectionId;
    }

    @Override
    public void process(Message msg) {
        try {
            switch (msg.opcode) {
                case 1: // register
                    if ( db.isRegistered(msg.arguments[0]) ){ //if the student has already registered
                        Message error1 = new Message((short) 11);
                        error1.shortArguments[0] = 1;
                        ConnectionsImpl.getInstance().send(connectionId, error1);
                    }
                    else {
                        db.addStudent(msg, connectionId);
                        Message msg1 = new Message((short) 10);
                        msg1.shortArguments[0] = 1;
                        ConnectionsImpl.getInstance().send(connectionId, msg1);
                    }
                    break;
                case 2: // login
                    Student st2 = db.getStudent(msg.arguments[0]);
                    if ( msg.binary == 1 && student == null &&
                            st2 != null && st2.isValidPassword(msg.arguments[1]) ){
                        setStudent(st2);
                        // send notification
                        ArrayDeque<Message> notifications = student.getNotifications();
                        while ( !notifications.isEmpty() )
                            ConnectionsImpl.getInstance().send(connectionId, notifications.poll());
                        student.clearNotification();
//                        Message note = student.getNotificationAfterTime(time);
//                        while ( note != null ){
//                            ConnectionsImpl.getInstance().send(connectionId, note);
//                            student.clearNote(note);
//                            note = student.getNotificationAfterTime(time);
//                        }

                        //send an ack
                        Message msg2 = new Message((short) 10);
                        msg2.shortArguments[0] = 2;
                        ConnectionsImpl.getInstance().send(connectionId, msg2);
                    }
                    else{
                        Message error2 = new Message((short) 11);
                        error2.shortArguments[0] = 2;
                        ConnectionsImpl.getInstance().send(connectionId, error2);
                    }
                    break;
                case 3: // logout
                    if ( student != null ){
                        student.logout();
                        this.student = null;

                        Message msg3 = new Message((short) 10);
                        msg3.shortArguments[0] = 3;
                        ConnectionsImpl.getInstance().send(connectionId, msg3);
                        ConnectionsImpl.getInstance().disconnect(connectionId);
                    }
                    else{
                        Message error3 = new Message((short) 11);
                        error3.shortArguments[0] = 3;
                        ConnectionsImpl.getInstance().send(connectionId, error3);
                    }
                    break;
                case 4: // follow/unfollow
                    //check if the student is logged in
                    if ( student == null ){
                        Message error4 = new Message((short) 11);
                        error4.shortArguments[0] = 4;
                        ConnectionsImpl.getInstance().send(connectionId, error4);
                    }
                    else {
                        if (msg.binary == 0) {
                            // check if one blocked the other
                            if (db.isBlocked(msg.arguments[0], student.userName))
                                throw new ArrayStoreException();  // the catch will send an ERROR
                            Student st4 = db.getStudent(msg.arguments[0]);
                            boolean follow = student.follow(st4);
                            if ( !follow )
                                throw new ArrayStoreException();  // the catch will send an ERROR
                        } else {
                            Student st = db.getStudent(msg.arguments[0]);
                            boolean unfollow = student.unfollow(st);
                            if (!unfollow)
                                throw new ArrayStoreException(); // the catch will send an ERROR
                        }

                        // send back an ACK Message
                        Message msg4 = new Message((short) 10);
                        msg4.shortArguments[0] = 4;
                        msg4.binary = msg.binary;
                        msg4.arguments[0] = msg.arguments[0];
                        ConnectionsImpl.getInstance().send(connectionId, msg4);
                    }
                    break;
                case 5: // post
                    if ( student != null ) {
                        ArrayList<String> usersTagged = findUsersTagged(msg.arguments[0]);
                        ArrayList<Student> users = findStudents(usersTagged);

                        String content5 = filterMassage(msg.arguments[0]);
                        student.addPost(); //increase the number of his posts
                        db.addPost(content5); // save the post
                        users.addAll(student.getFollowersNotTagged(users)); // add all the followers

                        Message msg5 = new Message((short) 10);
                        msg5.shortArguments[0] = 5;
                        ConnectionsImpl.getInstance().send(connectionId, msg5);

                        for (Student st : users) {
                            Message note5 = new Message((short) 9);
                            note5.binary = 1;
                            note5.arguments[0] = st.userName;
                            note5.arguments[1] = content5;
                            if ( st.isLogin() )
                                ConnectionsImpl.getInstance().send(st.getId(), note5);
                            else
                                st.addNotification(note5);
                        }
                    }
                    else{
                        Message error5 = new Message((short) 11);
                        error5.shortArguments[0] = 5;
                        ConnectionsImpl.getInstance().send(connectionId, error5);
                    }
                    break;
                case 6: // PM Message
                    if ( student != null ) { // check if the student is logged in
                        Student st6 = db.getStudent(msg.arguments[0]);
                        if ( st6 != null ) { // check if recipient user is registered
                            if ( student.isFollow(st6) ) { // check following and following time
                                String content6 = filterMassage(msg.arguments[1]);
                                db.addPost(content6); // save the message in the database

                                //send the Ack
                                Message msg6 = new Message((short) 10);
                                msg6.shortArguments[0] = 6;
                                ConnectionsImpl.getInstance().send(connectionId, msg6);
                                //send the notification
                                Message notif6 = new Message((short) 9);
                                notif6.binary = 0;
                                notif6.arguments[0] = st6.userName;
                                notif6.arguments[1] = content6;
                                if ( st6.isLogin() )
                                    ConnectionsImpl.getInstance().send(st6.getId(), notif6);
                                else
                                    st6.addNotification(notif6);
                            }
                        } else {
                            Message error6 = new Message((short) 11);
                            error6.shortArguments[0] = 6;
                            ConnectionsImpl.getInstance().send(connectionId, error6);
                        }
                    }
                    else{
                        Message error6 = new Message((short) 11);
                        error6.shortArguments[0] = 6;
                        ConnectionsImpl.getInstance().send(connectionId, error6);
                    }
                    break;
                case 7: // Log Stat
                    if ( student != null && db.isRegistered(student) ) {
                        for (Student s : db.studentTable.values()) {
                            if ( s.isLogin() && !db.isBlocked(student.userName, s.userName) ) {
                                Message msg7 = new Message((short) 10);
                                msg7.shortArguments = s.statsToByte((short) 7);
                                ConnectionsImpl.getInstance().send(connectionId, msg7);
                            }
                        }
                    }
                    else{
                        Message error7 = new Message((short) 11);
                        error7.shortArguments[0] = 7;
                        ConnectionsImpl.getInstance().send(connectionId, error7);
                    }
                    break;
                case 8: // Stat
                    if ( student != null && db.isRegistered(student) ){
                        ArrayList<Student> students = fromNameToStudent(msg.arguments[0]);

                        for (Student s : students){
                            if ( !db.isBlocked(student.userName, s.userName) ) {
                                Message msg8 = new Message((short) 10);
                                msg8.shortArguments = s.statsToByte((short) 8);
                                ConnectionsImpl.getInstance().send(connectionId, msg8);
                            }
                        }
                    }
                    else{
                        Message error8 = new Message((short) 11);
                        error8.shortArguments[0] = 8;
                        ConnectionsImpl.getInstance().send(connectionId, error8);
                    }
                    break;
                case 12: // block
                    boolean bl = db.block(msg.arguments[0], student.userName);
                    if ( !bl ) {
                        Message error12 = new Message((short) 11);
                        error12.shortArguments[0] = 12;
                        ConnectionsImpl.getInstance().send(connectionId, error12);
                    }
                    else{
                        Student s = db.getStudent(msg.arguments[0]);
                        student.unfollow(s);
                        s.unfollow(student);
                        Message msg12 = new Message((short) 10);
                        msg12.shortArguments[0] = 12;
                        ConnectionsImpl.getInstance().send(connectionId, msg12);
                    }
                    break;
            }
        }catch(Exception e){
            if ( e instanceof ArrayStoreException ) {
                Message error4 = new Message((short) 11);
                error4.shortArguments[0] = 4;
                ConnectionsImpl.getInstance().send(connectionId, error4);
            }
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    // return an array list of all users that are tagged in a content
    private ArrayList<String> findUsersTagged (String content){
        ArrayList<String> tags = new ArrayList<>();
        int start = 0;
        boolean found = false;
        int index = 0;

        while ( index < content.length() ){
            if ( content.charAt(index) == '@' ) {
                start = index + 1;
                found = true;
            }
            if ( found ) {
                while ( !(content.charAt(index) == ' ') && (index < content.length()) )
                    index++;
                String user = content.substring(start, index);
                if ( student.isFollow(db.getStudent(user)) )
                    tags.add(user);
                found = false;
            }
            index++;
        }
        return tags;
    }

    // return an array list of users that have already registered from a list of usernames
    private ArrayList<Student> findStudents (ArrayList<String> names){
        ArrayList<Student> users = new ArrayList<>();
        for (String s : names){
            Student st = db.getStudent(s);
            if ( st != null )
                users.add(db.getStudent(s));
        }
        return users;
    }

    // censors the message
    private String filterMassage (String message){
        String[] words = db.getWordsToFilter();
        for (int i = 0; i < words.length; i++)
            message = message.replaceAll(words[i], "<filtered>");

        return message;
    }

    // get a String of user-names seperated by '|' and return a list of all students
    // that are registered and have one of those names
    private ArrayList<Student> fromNameToStudent (String names){
        String[] studentsNames = names.split("\\|");
        ArrayList<Student> students = new ArrayList<>();
        for (int i = 0; i < studentsNames.length; i++){
            Student s = db.getStudent(studentsNames[i]);
            if ( s != null )
                students.add(s);
        }
        return students;
    }

}