package bgu.spl.net.objects;

import bgu.spl.net.objects.commands.Message;
import com.sun.org.apache.xpath.internal.objects.XNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static java.time.temporal.ChronoUnit.YEARS;

public class Student {
    private int id;
    public final String userName;
    public final String password;
    public LocalDate birthDay;
    public final int yearOfBirth;
    private ArrayList<Student> followingList = new ArrayList<>(); //saves all the users that the student follows
    private ArrayList<Student> followerList = new ArrayList<>(); //saves all the users that following the student
    private short numOfPosts = 0;
    private boolean isLogin = false;
    private ArrayDeque<Message> notifications = new ArrayDeque<>();
//    private ConcurrentSkipListMap<Timestamp, Message> notificationsByTIme = new ConcurrentHashMap<>();

    public Student(String userName, String password, String birthDay, int id){
        this.userName = userName;
        this.password = password;
        this.id = id;

        int year = Integer.parseInt(birthDay.substring(6));
        this.yearOfBirth = year;
//        int month = Integer.parseInt(birthDay.substring(3,5));
//        int day = Integer.parseInt(birthDay.substring(0,2));
//
//        this.birthDay = LocalDate.of(year, month, day);
    }

    public boolean follow (Student st){
        boolean isDone = true;
        if ( followingList.contains(st) )
            isDone = false;
        else {
            followingList.add(st);
            st.addFollower(this);
        }
        return isDone;
    }

    public void addFollower (Student st){
        followerList.add(st);
    }

    public boolean unfollow (Student st){
        boolean isDone = true;
        if ( !followingList.contains(st) )
            isDone = false;
        else {
            followingList.remove(st);
            st.removeFollower(this);
        }
        return isDone;
    }

    public void removeFollower(Student st){
        followerList.remove(st);
    }

    public boolean isValidPassword (String pw){
        return password.equals(pw);
    }

    public boolean isFollow (Student st){
        return followingList.contains(st);
    }

    public void addPost() { numOfPosts = (short) (numOfPosts + 1); }

    public short[] statsToByte(short op){
        short[] output = new short[6];

        output[0] = op;
        output[1] = getAge();
        output[2] = numOfPosts;
        output[3] = (short) followerList.size();
        output[4] = (short) followingList.size();

        return output;
    }

//    private short getAge(){
//        LocalDate today = LocalDate.now();
//        return (short) YEARS.between(birthDay, today);
//    }

    public short getAge(){
        return (short) (2021 - yearOfBirth);
    }

    public void login(){ isLogin = true; }

    public void logout() { isLogin = false; }

    public boolean isLogin(){ return isLogin; }

    //return an array list with all followers except the ones who is in 'list'
    public ArrayList<Student> getFollowersNotTagged(ArrayList<Student> list){
        ArrayList<Student> output = new ArrayList<>();
        for (Student s : followerList){
            if ( !list.contains(s) )
                output.add(s);
        }
        return output;
    }

    public Integer getId(){ return id; }

    public void setId (int id){ this.id = id; }

    public void addNotification (Message note){ notifications.add(note); }

    public ArrayDeque<Message> getNotifications() { return notifications; }

    public void clearNotification() { notifications.clear(); }

//    public void addNote(Message msg, Timestamp time){
//        notificationsByTIme.put(time, msg);
//    }
//
//    public void clearNote(Message msg){
//        notificationsByTIme.remove(msg);
//    }
//
//    public Message getNotificationAfterTime(Timestamp time){
//        return notificationsByTIme.get(notificationsByTIme.ceilingKey(time));
//    }

}