package Server;

import Message.FileTransceive;
import Message.ReqFileTransceive;
import Message.Respond;
import util.NetworkUtil;
import Client.*;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Server {

    private ServerSocket serverSocket;
    static Server server;
    public HashMap<String, NetworkUtil> clientMap;
    public static HashMap<Integer, FileTransceive> fileMap;
    public static HashMap<Integer, ReqFileTransceive> reqFileMap;
    public static HashMap<String,String> msg;
    public static List<String> msgList;
    public static int buffer_size=0;

    public static final int max_Buffer_Size = 1024 * 1024 * 100;
    public static final int min_chunk_Size = 1024 ;
    public static final int max_chunk_Size = 1024 * 4;

    Server() {

        clientMap = new HashMap<>();
        fileMap=new HashMap<>();
        reqFileMap=new HashMap<>();
        msg=new HashMap<>();
        msgList=new ArrayList<>();
        try {
            serverSocket = new ServerSocket(33333);
        } catch (Exception e) {
            System.out.println("Server starts:" + e);
        }
    }

    private void setFileMap(String s)
    {

            String folderPath = "E:\\CN_offline1\\"+s+"\\Public";
//            System.out.println("showing path: "+folderPath);
            File folder = new File(folderPath);
            File[] files = folder.listFiles();

//            System.out.println(files.length);

            for(int i=0;i<files.length;i++)
            {
                FileTransceive fileTransceive=new FileTransceive(Integer.toString((int) files[i].length()));
                fileTransceive.name=files[i].getName();
                fileTransceive.status="Public";
                fileTransceive.from=s;

                fileMap.put(fileMap.size()+1,fileTransceive);

            }
    }

    public void Connect() throws IOException, ClassNotFoundException {
        while (true) {
            Socket clientSocket = serverSocket.accept();
            serve(clientSocket);
        }
    }

    public void serve(Socket clientSocket) throws IOException, ClassNotFoundException {
        NetworkUtil networkUtil = new NetworkUtil(clientSocket);
        Client client= (Client) networkUtil.read();
        if(clientMap.containsKey(client.clientName) && clientMap.get(client.clientName).isActive)
        {
            System.out.println("already have this");
            networkUtil.write("already have this user.");
        }
        else
        {
            new ReadThreadServer(clientMap, networkUtil,this);
            System.out.println("login successful");
            File directory = new File("E:\\CN_offline1\\" + client.clientName);
            directory.mkdir();

            File pubDir=new File("E:\\CN_offline1\\" + client.clientName+"\\Public");
            pubDir.mkdir();
            File privateDir=new File("E:\\CN_offline1\\" + client.clientName+"\\Private");
            privateDir.mkdir();

            if(!clientMap.containsKey(client.clientName))
            {
                setFileMap(client.clientName);

                for(int i: reqFileMap.keySet())
                {
                    fileMap.put(fileMap.size()+1,reqFileMap.get(i));
                }
            }
            String msgList="";
            clientMap.put(client.clientName, networkUtil);

            System.out.println("print msg list");
            for(int i=0;i<Server.msgList.size();i++)
            {
                msgList+=Server.msgList.get(i)+"\n";
                System.out.println(Server.msgList.get(i));
            }
            msg.put(client.clientName,msgList);


            Respond respond=new Respond("Logged in");
            for (String i : clientMap.keySet()) {
                System.out.println("from server:");
                respond.add(i);
            }
            networkUtil.write(respond);


        }

        System.out.println(clientMap.keySet());


    }

    public synchronized static Server getServer()
    {
        if(server==null)
        {
//            System.out.println("nuull");
            server= new Server();
        }
        return server;
    }

    public static void main(String args[]) throws IOException, ClassNotFoundException {
        server = new Server();
        server.Connect();
    }
}
