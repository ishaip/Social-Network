package bgu.spl.net.objects.commands;

import java.util.Arrays;

public class Message {
    public short opcode;
    public byte binary = 0;
    public String[] arguments = {"","",""};
    public short[] shortArguments = new short[6];

    public Message(short opcode) {
        this.opcode = opcode;
    }

    @Override
    public String toString() {
        return "Message {" +
                "opcode = " + opcode +
                ", binary = " + binary +
                ", arguments = " + Arrays.toString(arguments) +
                ", shortArguments = " + Arrays.toString(shortArguments) +
                '}';
    }


}
