import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) 
    {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter message string: ");
        String message = scanner.nextLine();
        System.out.println("Enter number of data bytes:");
        int dataBytes = scanner.nextInt();
        System.out.println("Enter probability:");
        double probability = scanner.nextDouble();
//
        System.out.println("Enter polynomial:");
        String polynomial = scanner.next();

        DataLinkLayer dataLinkLayer=new DataLinkLayer(message,dataBytes);
        dataLinkLayer.formMessage();
        dataLinkLayer.formPolynomial(polynomial);


        System.out.println("data string after padding: " + dataLinkLayer.message);
        System.out.println();
        dataLinkLayer.formDataBlocks();

        System.out.println("data block <ascii code of m characters per row>: ");
        dataLinkLayer.printDataBlocks(dataLinkLayer.databits);
        System.out.println();

        dataLinkLayer.addCheckBits();

        System.out.println("data block after adding check bits: ");
        dataLinkLayer.printWithColor();
        System.out.println();
//
        dataLinkLayer.getSerializedData();
        //print serialized data
        System.out.println("data bits after column-wise serialization: ");
        dataLinkLayer.printSerializedData(false);
        System.out.println();
//
        int cnt=dataLinkLayer.addCRCCheckSum();
        System.out.println("data bits after appending CRC checksum <sent frame>: ");
        dataLinkLayer.printCRCInCyan(cnt);
        System.out.println();

        dataLinkLayer.toggleBit(probability);
        System.out.println("received frame: ");
        dataLinkLayer.printSerializedData(true);
        System.out.println();


        //check error
        System.out.print("result of CRC checksum matching: ");
        if(dataLinkLayer.hasError())
                System.out.println("error detected");

        else
                System.out.println("no error detected");

        System.out.println();

        System.out.println("data block after removing CRC checksum bits: ");
        dataLinkLayer.removeCRCCheckSum();

        ArrayList<Integer> cur[]=  dataLinkLayer.deSerialize();
        dataLinkLayer.printDataBlocksWithError(cur);
        System.out.println();

        dataLinkLayer.reconstruct(cur);

         cur=dataLinkLayer.removeCheckBits(cur);
        System.out.println("data block after removing checkbits: ");
        dataLinkLayer.printDataBlocks(cur);
        System.out.println();


        System.out.println("output frame: "+dataLinkLayer.getMessage(cur));
    }


    
}