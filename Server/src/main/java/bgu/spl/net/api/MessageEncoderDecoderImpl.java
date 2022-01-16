package bgu.spl.net.api;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import bgu.spl.net.objects.commands.Message;

public class MessageEncoderDecoderImpl implements MessageEncoderDecoder<Message> {
    private short opcode = -1;
    ArrayDeque<Byte> stringBuffer = new ArrayDeque<>();
    int argumentIndex = 0;
    private final ByteBuffer lengthBuffer = ByteBuffer.allocate(2);
    String[] arguments = new String[3];
    private byte[] bytes = new byte[1<<16];
    private int len = 0;
    private boolean byteHaseBeenRead = false;
    private byte byteArgument = 0;

    /*
    we will decode the information needed
    then according to the opcode we will decide what response and action are needed
    then we will
     */

    @Override
    public Message decodeNextByte(byte nextByte) {
        if (opcode == -1) {      //we still didn't get the opcode
            lengthBuffer.put(nextByte);
            if (!lengthBuffer.hasRemaining()) {  //when it has 2 arguments
                lengthBuffer.flip();
                opcode = lengthBuffer.getShort();     //extract the arguments and turn them into the opcode
                lengthBuffer.clear();
            }
        }
        else
            return decodeWithOpcode(nextByte);
        return null;
    }

    private Message decodeWithOpcode(byte nextByte){
        Message output = null;
        switch (opcode){
            case 7:
            case 3:     //in logout, logged status there are no arguments
                break;
            case 5:
            case 8:
            case 12:        //in post,stats,block there is 1 string argument
                if (nextByte == '\0')
                    arguments[0] = popString(arrayDequeToBytes(stringBuffer));

//                if (nextByte == ';'){
//                    output = new Message(opcode);
//                    output.arguments[0] = arguments[0];
//                    reset();
//                    break;
//                }
                stringBuffer.add(nextByte);
                break;
            case 4:     //in follow there is 1 string argument and 1 byte argument
                if (!byteHaseBeenRead)
                    byteArgument = nextByte;
                if (nextByte == '\0' && byteHaseBeenRead) {
                    arguments[argumentIndex++] = popString(arrayDequeToBytes(stringBuffer));
                    stringBuffer.clear();
                }
                if(byteHaseBeenRead)
                    stringBuffer.add(nextByte);
                byteHaseBeenRead = true;
                break;
            case 2:     //in login there is 2 string arguments AND THEN 1 byte argument
                if ( argumentIndex == 2 && nextByte != ';' && nextByte != '\0' )    //if 2 argument have read we are now receiving the bit argument
                    byteArgument = nextByte;
                else if ( nextByte != '\0' )
                    stringBuffer.add(nextByte);
                if (nextByte == '\0') {
                    arguments[argumentIndex++] = popString(arrayDequeToBytes(stringBuffer));
                    stringBuffer.clear();
                }
                break;
            case 9:     //in notification the order of arguments is reversed
                if (!byteHaseBeenRead)   //the fist byte sent after the opcode is this instance is the byte argument
                    byteArgument = nextByte;
                if (byteHaseBeenRead && nextByte == '\0') {
                    arguments[argumentIndex++] = popString(arrayDequeToBytes(stringBuffer));
                    stringBuffer.clear();
                }
                else if (byteHaseBeenRead)
                    stringBuffer.add(nextByte);
                byteHaseBeenRead = true;
                break;
            case 1:
            case 6:     // in register,PM there are 3 string arguments
                if (nextByte == '\0') {
                    arguments[argumentIndex++] = popString(arrayDequeToBytes(stringBuffer));
                    stringBuffer.clear();
                }
                else
                    stringBuffer.add(nextByte);
                break;
        }
        if (nextByte == ';'){
            output = new Message(opcode);
            output.binary = byteArgument;
            output.arguments = arguments.clone();
            //System.out.println("decoder\n" + output);
            reset();
        }
        return output;
    }

    private void reset(){
        opcode= -1;
        stringBuffer.clear();
        argumentIndex =0;
        arguments= new String[3];
        bytes = new byte[1<<16];
        len = 0;
        lengthBuffer.clear();
        byteHaseBeenRead = false;
        byteArgument = 0;
    }

    private static byte[] arrayDequeToBytes(ArrayDeque<Byte> list){
        int index = 0;
        byte[] output = new byte[list.size()];
        for (byte b: list )
            output[index++] = b;
        return output;
    }

    private static byte[] shortToBytes(short s){
        byte[] output = new byte[2];
        output[1] = (byte)(s & 0xff);
        output[0] = (byte)((s >> 8) & 0xff);
        return output;
    }

    public static short bytesToShort(byte[] byteArr)
    {
        short result = (short)((byteArr[0] & 0xff) << 8);
        result += (short)(byteArr[1] & 0xff);
        return result;
    }

    private static void arrayDequeAddAll(ArrayDeque<Byte> into, byte[] from){
        for(Byte b: from)
            into.addLast(b);
    }

    private String popString(byte[] bytes){
        return new String(bytes, 0, bytes.length, StandardCharsets.UTF_8);
    }

    private void pushByte(byte nextByte){
        if ( len >= bytes.length )
            bytes = Arrays.copyOf(bytes, len *2);
        bytes[len++] = nextByte;
    }

    @Override
    public byte[] encode(Message message) {
        reset();
        ArrayDeque<Byte> output = new ArrayDeque<>();
        opcode = message.opcode;
        arrayDequeAddAll(output,shortToBytes(opcode));
        switch (opcode){
            case 9:     //notification   1 binary argument and 2 string arguments
                output.addLast(message.binary);
                arrayDequeAddAll(output,message.arguments[0].getBytes(StandardCharsets.UTF_8));
                output.addLast((byte)'\0');
                arrayDequeAddAll(output,message.arguments[1].getBytes(StandardCharsets.UTF_8));
                output.addLast((byte)'\0');
                break;
            case 10:    //ack
                arrayDequeAddAll(output,shortToBytes(message.shortArguments[0]));
                if (message.shortArguments[0] == 4 ){       // acknowledging a follow message has 1 string
                    arrayDequeAddAll(output,message.arguments[0].getBytes(StandardCharsets.UTF_8));
                    output.addLast((byte)'\0');
                }
                else if ( message.shortArguments[0] == 7 || message.shortArguments[0] == 8 ) {       //acknowledging a log stat, stat both of them have 5 short arguments
                    for(int i = 1; i < 5; i++){
                        arrayDequeAddAll(output,shortToBytes(message.shortArguments[i]));
                    }
                }

                break;
            case 11:    //error     //only 1 short argument
                arrayDequeAddAll(output,shortToBytes(message.shortArguments[0]));
                break;
        }
        output.addLast((byte) ';');
        //System.out.println("encoder: " + message);
        reset();
        return arrayDequeToBytes(output);
    }
}