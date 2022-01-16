package bgu.spl.net.objects;

import bgu.spl.net.objects.commands.Message;

import java.util.ArrayList;
import java.util.HashMap;

public class DataBase {
    private static boolean isDone = false;
    private static DataBase instance;
    public HashMap<String, Student> studentTable = new HashMap<>();
    private HashMap<String, String> blockedStudents = new HashMap<>();
    private final static String[] wordsToFilter = {"war", "Trump", "Assignment2"};
    private ArrayList<String> posts = new ArrayList<>();


    public static DataBase getInstance(){
        if (!isDone){
            synchronized (DataBase.class){
                if (!isDone){
                    instance = new DataBase();
                    isDone = true;
                }
            }
        }
        return instance;
    }

    public void addStudent (Message msg, int id){
        Student student = new Student(msg.arguments[0], msg.arguments[1], msg.arguments[2], id);
        studentTable.put(student.userName, student);
    }

    public boolean block (String st1, String st2){
        boolean isDone = true;
        if ( !studentTable.containsKey(st1) || !studentTable.containsKey(st2) )
            isDone = false;
        else {
            blockedStudents.putIfAbsent(st1, st2);
            blockedStudents.putIfAbsent(st2, st1);
        }
        return isDone;
    }

    public boolean isBlocked (String st1, String st2){
        boolean conditionA =  blockedStudents.containsKey(st1) && (blockedStudents.get(st1).equals(st2));
        boolean conditionB =  blockedStudents.containsKey(st2) && (blockedStudents.get(st1).equals(st1));
        return conditionA || conditionB;
    }

    public Student getStudent (String userName){
        return studentTable.get(userName);
    }

    public String[] getWordsToFilter (){ return wordsToFilter; }

    public boolean isRegistered (Student student){
        return studentTable.containsValue(student);
    }

    public boolean isRegistered (String name){
        return studentTable.containsKey(name);
    }

    public void addPost (String content){ posts.add(content); }
}
