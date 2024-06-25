package Client;

import util.NetworkUtil;
import Message.*;

import java.io.*;
import java.util.Scanner;

public class ReadThreadClient implements Runnable {
    private Thread thr;
    private NetworkUtil networkUtil;
    public String name;
    public boolean isReq;

    public ReadThreadClient(NetworkUtil networkUtil,String n) throws IOException {
        this.networkUtil = networkUtil;
        name=n;
        this.thr = new Thread(this);
        thr.start();
    }

    public void run() {
        try {
            while (true) {

                Object o = networkUtil.read();
                if(o==null)
                {
//                    PrintMenu();
                    System.out.println("why null");
                }
                 if(o instanceof Respond)
                {
                  Respond respond=(Respond) o;
                    if(respond.getRespond().equalsIgnoreCase("Sending the list"))
                    {
                      System.out.println("Client list: ");
                        for(int i=0;i<respond.getList().size();i++)
                        {
                            System.out.println(respond.getList().get(i));
                        }
                    }
                   else if(respond.getRespond().equalsIgnoreCase("Sending file list"))
                    {
                        System.out.println("File list: ");
                        for(int i=0;i<respond.getList().size();i++)
                        {
                            System.out.println(respond.getList().get(i));
                        }
                        
                    }
                    else if(respond.getRespond().equalsIgnoreCase("Sending other client's file list"))
                    {
                        System.out.println("Client's File list: ");
                        for(int i=0;i<respond.getList().size();i++)
                        {
                            System.out.println(respond.getList().get(i));
                        }
                    }

                    else if(respond.getRespond().equalsIgnoreCase("File size is too large"))
                    {
                        System.out.println("File size is too large");
                    }

                    else if(respond.getRespond().equalsIgnoreCase("File upload request accepted"))
                    {
                        isReq=true;
                        networkUtil.socket.setSoTimeout(30000);
                        System.out.println("File upload request accepted from client side");
                        File upFile=new File("E:\\CN_offline1\\" + respond.getList().get(respond.getList().size()-2)+"\\"+respond.getList().get(0));

                        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(upFile));
                        FileTransceive file=new FileTransceive(respond.getList().get(respond.getList().size()-3).toString());

                        int bytesRead=0;
                        while ((bytesRead = bufferedInputStream.read(file.buffer)) != -1) {
                            networkUtil.write(new FileTransceive(file.buffer, file.chunk_size, bytesRead, (String) respond.getList().get(0), (String) respond.getList().get(respond.getList().size()-1), (String) respond.getList().get(respond.getList().size()-2), (int) upFile.length(),respond.to));

                        }
                       // System.out.println("outside the loop");
                        Request request=new Request("Full file uploaded successfully");
                        request.setTo(respond.to);
                        request.setFrom( (String) respond.getList().get(respond.getList().size()-2));
                        networkUtil.write(request);
                    }

                    else if(respond.getRespond().equalsIgnoreCase("uploading"))
                    {
                        System.out.println(respond.desc);
                    }

                    else if(respond.getRespond().equalsIgnoreCase("sending unread message"))
                    {
                        System.out.println("All unread messages:");
                        for(int i=0;i<respond.getList().size();i++)
                        {
//                            System.out.println(respond.getList().get(i));

                            if(respond.getList().size()==1 && respond.getList().get(i).equals(""))
                                break;
//                            isReq=true;
                            if(respond.getList().get(i).equals("") )
                                continue;
                            if(respond.getList().get(i).equals("A file has been uploaded in response to your request"))
                            {
                                System.out.println(respond.getList().get(i));
                                continue;
                            }
                            System.out.println(respond.getList().get(i));
                            System.out.println("Do you want to upload a file in response to this request?\n1.yes\n2.no");
                            Scanner sc=new Scanner(System.in);
                            int opt=sc.nextInt();


                            if(opt==1)
                            {
                                String[] from=respond.getRespond().split(" ");
                                isReq=true;
                                networkUtil.FileUpload(name,from[1],true);
                            }
                        }

                        for(int i=0;i<respond.getList().size();i++)
                            respond.getList().remove(i);



                        Request request=new Request("got my unread messages");
                        request.setFrom(name);

                        networkUtil.write(request);
                    }

                    else if(respond.getRespond().equalsIgnoreCase("sendiing all uploaded files"))
                    {
                        System.out.println("got all up files. Select the desired file");
                        for(int i=0;i<respond.getList().size();i+=4)
                        {

                            System.out.println(respond.getList().get(i)+"."+respond.getList().get(i+1)+" Owner: " +respond.getList().get(i+2)+" Privacy: "+respond.getList().get(i+3));
                        }
                        Scanner input=new Scanner(System.in);

                        int n=input.nextInt();
                        isReq=true;


                        //download

                        System.out.println("start downloading...");
                        ReqFileTransceive ft=new ReqFileTransceive("0","Download the file");
                        ft.name=respond.getList().get(4*n-3).toString();
                        ft.from=respond.getList().get(4*n-2).toString();
                        ft.status=respond.getList().get(4*n-1).toString();
                        networkUtil.write(ft);
                    }

                }

                else if(o instanceof String)
                {
                    String str=(String) o;
                    System.out.println(str);
                    if(str.equalsIgnoreCase("full upload done"))
                    {
                        networkUtil.socket.setSoTimeout(0);
                        isReq=false;
                        System.out.println("A file has been uploaded ");
                    }
                    if(str.equalsIgnoreCase("already have this user."))
                    {
                        System.out.println("This username is already in use. You can't login");
                        networkUtil.closeConnection();
                        System.exit(0);
                    }
                    else if(str.equalsIgnoreCase("all message showed"))
                    {
                        System.out.println("bleh!");
                    }

                }
                else if(o instanceof FileTransceive)
                {
                    FileTransceive ft=(FileTransceive) o;
//                    System.out.println("Down Info: "+ ft.from+" to:"+name);

                    FileInputStream inputStream = new FileInputStream("E:\\CN_offline1\\"+ft.from+"\\"+ft.status+"\\"+ft.name );
                    FileOutputStream outputStream = new FileOutputStream("E:\\CN_offline1\\"+name+"\\"+ft.name);

                    // Copy the file
                    ft.from=name;
                    byte[] buffer = new byte[ft.chunk_size];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                   // Message message=new Message("Download completed");
                    System.out.println("download done");
                    isReq=false;

                    inputStream.close();
                    outputStream.close();
                }

                if(!isReq)
                    PrintMenu();

            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try {
                networkUtil.closeConnection();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void PrintMenu() throws IOException {
        Scanner input=new Scanner(System.in);
        String from = name;
        System.out.println("Enter the command");
        System.out.println("1. Look up Client List");
        System.out.println("2.Show My Files");
        System.out.println("3.Show other Files");
        System.out.println("4. Upload File");
        System.out.println("5. Download File");
        System.out.println("6.Request a file");
        System.out.println("7. Show my Messages");
        System.out.println("8. DisConnect");

        Scanner scanner = new Scanner(System.in);
        int option = scanner.nextInt();
        if (option == 1) {
            System.out.println("printing the list:");
            Request req=new Request("print list");
            req.setFrom(name);
            networkUtil.write(req);

        }
        else if(option==2)
        {
            System.out.println("My File list:");
//                    Message req=new Message();
            Request req=new Request("show my files");
            req.setFrom(name);
            networkUtil.write(req);

        }
        else if(option==3)
        {
            System.out.println("Enter the client name:");
            String clientName=input.nextLine();
//                    Message req=new Message();
            Request req=new Request("show other files");
            req.setFrom(name);
            req.setTo(clientName);
            networkUtil.write(req);
        }
        else if(option==4)
        {
            networkUtil.FileUpload(from,name,false);

        }
        else if(option==5)
        {
            Request request=new Request("all uploaded file");
            request.setFrom(from);

            networkUtil.write(request);
        }
        else if(option==6)
        {
            System.out.println("Write a description for the requested file:");
            String des=input.nextLine();

            Request request=new Request("request for a file");
            request.desc=des;
            request.setFrom(from);

            networkUtil.write(request);
        }
        else if(option==7)
        {
//            System.out.println("All unread messages req sent");
            Request req=new Request("show unread message");
//            System.out.println(from);
            req.setFrom(name);

            networkUtil.write(req);
        }
        else if(option==8)
        {
            String status;
            status="Not active";
            Client.clientList.put(name,status);
            networkUtil.write(Client.clientList);
            networkUtil.closeConnection();
            System.exit(0);

        }
    }
}



