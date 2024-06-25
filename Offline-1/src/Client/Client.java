package Client;

import util.NetworkUtil;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Scanner;

public class Client implements Serializable {

    public String clientName,status;
    public static HashMap<String,String> clientList;
    public Client(String clientName)
    {
        this.clientName=clientName;
    }
    public Client(String name, String status)
    {
        clientName=name;
        this.status=status;
    }

    public Client(String serverAddress, int serverPort) {
        try {
            clientList=new HashMap<>();
            System.out.print("Enter name of the client: ");
            Scanner scanner = new Scanner(System.in);
            String clientName = scanner.nextLine();
            Client c=new Client(clientName);
            c.status="Active";
            NetworkUtil networkUtil = new NetworkUtil(serverAddress, serverPort);
            networkUtil.write(c);
            new ReadThreadClient(networkUtil,clientName);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String args[]) {
        String serverAddress = "127.0.0.1";
        int serverPort = 33333;
        Client client = new Client(serverAddress, serverPort);
    }
}


