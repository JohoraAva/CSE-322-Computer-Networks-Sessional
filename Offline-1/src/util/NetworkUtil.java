package util;


import Message.Request;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class NetworkUtil implements Serializable {
    public Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    public boolean isActive;

    public NetworkUtil(String s, int port) throws IOException {
        isActive = true;
        this.socket = new Socket(s, port);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
    }


    public NetworkUtil(Socket s) throws IOException {
        isActive = true;
        this.socket = s;
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
    }

    public Object read() throws IOException, ClassNotFoundException {
        return ois.readUnshared();
    }

    public void write(Object o) throws IOException {
        oos.writeUnshared(o);
    }

    public void FileUpload(String from, String name, boolean isReq) throws IOException {
        Scanner input = new Scanner(System.in);
        System.out.println("Enter the file name:");
        String fileName = input.nextLine();
        File file = new File("E:\\CN_offline1\\" + from + "\\" + fileName);
        int size = (int) file.length();
        System.out.println(from + "from to =" + name);
        String fileCondition = "Public";
        Request req = new Request("");
        req.desc = "upload file in response to a request";
        if (!isReq) {
            req.desc = "upload file";
            System.out.println("Is the file public?\n1.yes\n2.no");
            int opt = input.nextInt();
            if (opt == 1) {
                fileCondition = "Public";
            } else {
                fileCondition = "Private";
            }
        }

        req.setFrom(from);
        req.setRequest("upload file " + from + " " + fileName + " " + size + " " + fileCondition);
        req.setTo(name); //req from
        write(req);
    }


    public void closeConnection() throws IOException {
        isActive = false;
        ois.close();
        oos.close();
    }
}

