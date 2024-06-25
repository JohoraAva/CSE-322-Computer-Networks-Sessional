package Server;

import util.NetworkUtil;
import Message.*;

import java.io.*;
import java.util.HashMap;
import java.util.Random;
import static java.lang.System.out;

public class ReadThreadServer implements Runnable {
    private Thread thr;
    private Server server;
    private NetworkUtil networkUtil;
    public HashMap<String, NetworkUtil> clientMap;


    public ReadThreadServer(HashMap<String, NetworkUtil> map, NetworkUtil networkUtil,Server s) {
        server=s;
        this.clientMap = map;
        this.networkUtil = networkUtil;
        this.thr = new Thread(this);
        thr.start();
    }

    public void run() {
        try {
            while (true) {
                Object o = networkUtil.read();
                if (o instanceof Request) {
                    Request obj = (Request) o;
                    String text=obj.getRequest();
                    String from=obj.getFrom();
//                    System.out.println("got req1");
                    out.println(text);
                    if(text.equalsIgnoreCase("print list"))
                    {
                        out.println("got req");
                        out.println(text+ " ;"+from);
                        NetworkUtil nu = clientMap.get(from);
                        if (nu != null) {
//                            out.println("nu not null");
                            Respond respond=new Respond("Sending the list");
                            for (String i : clientMap.keySet()) {
                                NetworkUtil temNet=clientMap.get(i);
                                String status;
                               if(temNet.isActive)
                               {
                                   status="Online";
                               }
                               else
                                   status="Offline";

                                out.println("from server read thread:");
                                respond.add(i+" Status: "+status);
                            }

                            nu.write(respond);
                        }
                    }
                    else if(text.equalsIgnoreCase("show my files"))
                    {
                        File file=new File("E:\\CN_offline1\\"+from+"\\Public");
                        File file2=new File("E:\\CN_offline1\\"+from+"\\Private");
                       String[] list=file.list();
                       String[] list2=file2.list();

                       Respond respond=new Respond("Sending file list");
                        NetworkUtil nu = clientMap.get(from);
                       for(String i:list)
                       {
                           if (nu != null) {
                               respond.add(i+ " Status= Public");
                           }
                       }
                        for(String i:list2)
                        {
                            if (nu != null) {
                                respond.add(i+ " Status= Private");
                            }
                        }
                       nu.write(respond);
                    }
                    else if(text.equalsIgnoreCase("show other files"))
                    {
                        String to=obj.getTo();
                        File file=new File("E:\\CN_offline1\\"+to+"\\Public");
                        String[] list=file.list();

                        NetworkUtil nu = clientMap.get(from);
                        Respond respond=new Respond("Sending other client's file list");
                        for(String i:list)
                        {
                            out.println(i);
                             if (nu != null) {
                                 respond.add(i);

                           }
                    }
                        nu.write(respond);
                    }

                    else if(text.substring(0,11).equalsIgnoreCase("upload file"))
                    {
                        String[] info=obj.getRequest().split(" ");
//                        System.out.println("kire?"+ info.length+ ":"+info[info.length-2]);
                        int size=Integer.valueOf(info[info.length-2]);

                        if(size+Server.buffer_size > Server.max_Buffer_Size)
                        {
                            Respond respond=new Respond("File size is too large");
                            networkUtil.write(respond);
                        }
                        else
                        {
                            out.println("hoy?"+obj.getTo());
                            Random rand=new Random();
                            int  chunk_size=rand.nextInt((server.max_chunk_Size-server.min_chunk_Size)+server.min_chunk_Size);
                            int id=Server.fileMap.size()+1;

                            Respond respond=new Respond("File upload request accepted");
                            respond.to=obj.getTo();
                            respond.add(info[3]);
                            respond.add(String.valueOf(id));
                            respond.add(size);
                            out.println("chunking "+ chunk_size);
                            respond.add(String.valueOf(chunk_size));
                            respond.add(info[2]);
                            respond.add(info[5]);

                            out.println("Checking respond desc:");
                            out.println(obj.desc);

                            for(String s: clientMap.keySet())
                            {
                                NetworkUtil nu=clientMap.get(s);
                                if(!s.equalsIgnoreCase(obj.getTo()))
                                {

//                                    out.println("koi?");
                                    String msgList=Server.msg.get(s);
                                    msgList=msgList+"\n"+"A file has been uploaded in response to your request";
                                    Server.msg.put(s,msgList);
                                    break;
                                }
                            }

                            networkUtil.write(respond);

                        }
                    }
                    else if(text.equalsIgnoreCase("show unread message"))
                    {
                        Respond respond=new Respond("sending unread message");


                        out.println("checking before:");
                        out.println(from);
                        String msgList=Server.msg.get(from);
                        String[] lines=msgList.split("\n");

                        for(int i=0;i<lines.length;i++)
                        {
                            respond.add(lines[i]);
                            if(!lines[i].equalsIgnoreCase("A file has been uploaded in response to your request"))
                                Server.msgList.add(lines[i]);
                        }
//                        respond.add();
                        out.println(msgList);
                        out.println("msg list check: ");
                        for(String s : Server.msg.keySet())
                        {
                            out.println(Server.msg.get(s));
                        }

                        networkUtil.write(respond);
                    }

                 else if(text.equalsIgnoreCase("got my unread messages"))
                    {
                        String msgList="";
                        Server.msg.put(obj.getFrom(),msgList);
                        out.println("previous msg deleted");
                    }

                    else if(text.equalsIgnoreCase("all uploaded file"))
                    {
                        out.println("sendiing all uploaded files="+ Server.fileMap.size());

                        Respond respond=new Respond("sendiing all uploaded files");
                        for(int i: Server.fileMap.keySet())
                        {
                            respond.add(i);
                            respond.add(Server.fileMap.get(i).name);
                            respond.add(Server.fileMap.get(i).from);
                            respond.add(Server.fileMap.get(i).status);
                        }

                        networkUtil.write(respond);
                    }
                    else if(text.equalsIgnoreCase("Full file uploaded successfully"))
                    {
                        networkUtil.write("full upload done");

                    }

                    else if(text.equalsIgnoreCase("request for a file"))
                    {
                        Respond respond=new Respond("User "+from+" Has requested for a file.");
                        respond.desc=obj.desc;
//                        respond.from=from;
                        System.out.println(respond.desc);

                       for(String s: Server.msg.keySet())
                       {
                           NetworkUtil nu=clientMap.get(s);
                           if(!s.equalsIgnoreCase(from))
                           {
                               String m=Server.msg.get(s);
                               Server.msg.put(s,m+"\n"+respond.getRespond()+":"+respond.desc);
                             //  nu.write(respond);
                           }
                           else
                           {
                               nu.write("your request has been sent");
//                               out.println("ggwp :')");
                           }

                       }




                        Server.reqFileMap.put(Server.reqFileMap.size()+1,new ReqFileTransceive(Integer.toString(0),obj.desc));
                    }

                }

                else if(o instanceof ReqFileTransceive)
                {
                    ReqFileTransceive rft=(ReqFileTransceive) o;
                    int idx=0;
                    if(rft.des.equalsIgnoreCase("Download the file"))
                    {
                        for(int i: Server.fileMap.keySet())
                        {
                            if(Server.fileMap.get(i).name.equalsIgnoreCase(rft.name))
                            {
                                idx=i;
                                break;
                            }
                        }
                    }

                    System.out.println("idx="+idx);

                    FileTransceive ft=Server.fileMap.get(idx);
                    out.println("sending downloading file....by chunk by chunk");
                    File upFile=new File("E:\\CN_offline1\\"+ft.from+"\\"+ft.status+"\\"+ft.name);

                    BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(upFile));

                    int bytesRead=0;
                    while ((bytesRead = bufferedInputStream.read(ft.buffer)) != -1) {
                        networkUtil.write(new FileTransceive(ft.buffer, ft.chunk_size, bytesRead, ft.name,ft.status,ft.from ,ft.total_size,ft.to));

                    }
                }
                else if(o instanceof  String)
                {
                    String str=(String) o;
                    if(str.equalsIgnoreCase("all message showed"))
                    {
                        networkUtil.write("all message showed");
//                        System.out.println("bleh!");
                    }
                }
                else if(o instanceof FileTransceive)
                {
                    FileTransceive ft=(FileTransceive) o;

                    FileInputStream inputStream = new FileInputStream("E:\\CN_offline1\\"+ft.from+"\\"+ft.name);
                    FileOutputStream outputStream = new FileOutputStream("E:\\CN_offline1\\"+ft.from+"\\"+ft.status+"\\"+ft.name );

                    // Copy the file
                    byte[] buffer = new byte[ft.chunk_size];
                    int bytesRead,cnt=ft.total_size;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        //add msg
                        Respond respond=new Respond("Uploading");
                        respond.desc=(ft.total_size-cnt+ft.chunk_size)+ "bytes are uploaded";
                        if(cnt-ft.chunk_size>=0)
                            cnt-=ft.chunk_size;
//                        out.println("cnt="+cnt);
                        networkUtil.write(respond);
                    }

                    inputStream.close();
                    outputStream.close();

                    out.println("size =="+bytesRead+" total="+ft.total_size);
                    if(cnt>=0)
                    {
//                        networkUtil.write("full upload done");
                        out.println("full file uploaded successfully");
                    }
                    else
                    {
                        out.println("Error");
                    }

                }
            }
        } catch (Exception e) {
            out.println(e);
        } finally {
            try {
                networkUtil.closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}



