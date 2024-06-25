import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DataLinkLayer
{
    final int BYTESIZE=8;
    String message;
    int dataBytes;//m
    ArrayList<Integer>[] databits;
    List<Integer> serializedData;
    List<Integer> polynomialBits;
    List<Integer> errorIdx;

    public DataLinkLayer(String message, int dataBytes)
    {
        this.message = message;
        this.dataBytes = dataBytes;

    }

    public void formMessage()
    {
        while(message.length()%dataBytes != 0)
        {
            message += "~";
        }
        databits=new ArrayList[message.length()/dataBytes];
        for(int i=0;i<databits.length;i++)
            databits[i]=new ArrayList<>();
    }
    public void formPolynomial(String polynomial)
    {
        polynomialBits=new ArrayList<>();
        for(int i=0;i<polynomial.length();i++)
        {
            polynomialBits.add(Integer.parseInt(polynomial.charAt(i)+""));
        }
    }

    void formDataBlocks()
    {

        int k=-1;
        for(int i=0;i<message.length();i++)
        {
            //ascii to binary string
            char ch=message.charAt(i);
            int asciiCode=(int)ch;
            String binary=Integer.toBinaryString(asciiCode);
            if(i%dataBytes==0)
                k++;
            //length=8
            while(binary.length()!=BYTESIZE)
            {
                binary="0"+binary;
            }

            //string to list
            List<Integer> bits=new ArrayList<>();
            for(int j=0;j<binary.length();j++)
            {
                bits.add(Integer.parseInt(binary.charAt(j)+""));
            }

            //add to data block
           for(int j=0;j<BYTESIZE;j++)
           {
                databits[k].add(bits.get(j));
           }
        }

    }

    void printDataBlocks(ArrayList<Integer>[] databits)
    {
        for(int t=0;t<databits.length;t++)
        {
//            System.out.println(databits[t]);
            for(int j=0;j<databits[t].size();j++)
            {
                System.out.print(databits[t].get(j));
            }
            System.out.println();
        }
    }


    void addCheckBits()
    {
        int checkBits=calculateCheckBits();
        for(int i=0;i<databits.length;i++) {
            for (int j = 0; j < databits[i].size(); j++) {
                if (isPowerOfTwo(j + 1))
                {
                    databits[i].add(j, 0);
                }
            }
        }

        for(int i=0;i<databits.length;i++) {

            for(int j=0;j<checkBits;j++)
            {
                int mask=1<<j;
                int parity=0;
                for(int k=0;k<databits[i].size();k++)
                {
                    if(((k+1)&mask)!=0)
                    {
                        parity^=databits[i].get(k);
                    }
                }
                databits[i].set( (mask-1),parity);

            }
        }

    }

    boolean isPowerOfTwo(int n)
    {
        return (int)(Math.ceil((Math.log(n) / Math.log(2)))) == (int)(Math.floor(((Math.log(n) / Math.log(2)))));
    }

    int calculateCheckBits()
    {
        int checkBits=0;
        while(Math.pow(2,checkBits)<dataBytes*BYTESIZE+checkBits+1)
        {
            checkBits++;
        }
        return checkBits;
    }

  
    

    void printWithColor()
    {
       for(int i=0;i<databits.length;i++)
       {
           for(int j=0;j<databits[i].size();j++)
           {
               if(isPowerOfTwo(j+1))
               {
                   System.out.print("\u001B[32m"+databits[i].get(j)+"\u001B[0m");
               }
               else
               {
                   System.out.print(databits[i].get(j));
               }
           }
           System.out.println();
       }
    }

    void getSerializedData()
    {
        List<Integer> serializedData=new ArrayList<>();
        int row=databits.length;
        int col=databits[0].size();
        for(int i=0;i<col;i++)
        {
            for(int j=0;j<row;j++)
            {
                serializedData.add(databits[j].get(i));
            }
        }
        this.serializedData=serializedData;

    }

    List<Integer> copySerializedData(List<Integer> list)
    {
        List<Integer> temp=new ArrayList<>();
        for(int i=0;i<list.size();i++)
        {
            temp.add(list.get(i));
        }
        return temp;

    }

    int addCRCCheckSum()
    {
        List<Integer>temp=copySerializedData(serializedData);
        int CRCbitPos=0;
        for(int i=0;i<polynomialBits.size()-1;i++)
        {
            temp.add(0);
        }

        for(int i=0;i<temp.size()-polynomialBits.size()+1;i++)
        {
            if(temp.get(i)==1)
            {
                for(int j=i;j<i+polynomialBits.size();j++)
                {
                    temp.set(j,temp.get(j) ^ (polynomialBits.get(j - i) ));
                }
            }
        }

        CRCbitPos=serializedData.size();
         for(int i=temp.size()-polynomialBits.size()+1;i<temp.size();i++)
        {
            serializedData.add(temp.get(i));
        }

        return CRCbitPos;

    }

    void toggleBit(double p)
    {
        errorIdx=new ArrayList<>();
        Random random=new Random();
        for(int i=0;i<serializedData.size();i++)
        {
            double probability=random.nextDouble();
            if(probability<=p)
            {
                errorIdx.add(i);
                serializedData.set(i,serializedData.get(i)^1);
            }
        }
    }

    void printSerializedData(boolean isColored)
    {
        for(int i=0;i<serializedData.size();i++)
        {
            if(isColored && errorIdx.contains(i))
            {
                System.out.print("\u001B[31m"+serializedData.get(i)+"\u001B[0m");
            }
            else
            {
                System.out.print(serializedData.get(i));
            }
        }
        System.out.println();
    }
    void printCRCInCyan(int pos)
    {
        for(int i=0;i<serializedData.size();i++)
        {
            if(i>=pos)
            {
                System.out.print("\u001B[36m"+serializedData.get(i)+"\u001B[0m");
            }
            else
            {
                System.out.print(serializedData.get(i));
            }
        }
        System.out.println();
    }

    boolean hasError()
    {
        List<Integer>temp=copySerializedData(serializedData);
        for(int i=0;i<temp.size()-polynomialBits.size()+1;i++)
        {
            for(int j=i;j<i+polynomialBits.size();j++)
            {
                if(temp.get(j)==1)
                {
                    temp.set(j,temp.get(j) ^ (polynomialBits.get(j - i) ));
                }
            }
        }

        //remainder!=0,error
        for(int i=0;i<temp.size();i++)
        {
            if(temp.get(i)==1)
                return false;
        }

        return true;
    }

    void colToRow()
    {
        for(int i=0;i<errorIdx.size();i++)
        {
            int row=errorIdx.get(i)/databits[0].size();
            int col=errorIdx.get(i)%databits[0].size();
            errorIdx.set(i, row*databits[0].size()+col);;
        }
    }

        
    void removeCRCCheckSum()
    {

        int pos=serializedData.size()-polynomialBits.size()+1;
        int size=serializedData.size();
        for(int i=pos;i<size;i++)
        {
            serializedData.remove(pos);
        }
    }

    ArrayList<Integer>[] deSerialize()
    {

         ArrayList<Integer> []temp=new ArrayList[databits.length];
        for(int i=0;i<databits.length;i++)
            temp[i]=new ArrayList<>();

        //update erroIdx
        colToRow();

       int row=0,col=0;
        for(int i=0;i<serializedData.size();i++)
        {
            temp[row++].add(col,serializedData.get(i));
            if(row>=databits.length)
            {
                row=0;
                col++;
            }
        }
        return temp;

    }

    void printDataBlocksWithError(ArrayList<Integer> databits[])
    {
        for(int t=0;t<databits.length;t++)
        {
//            System.out.println(databits[t]);
            for(int j=0;j<databits[t].size();j++)
            {
//                System.out.println("chk again: "+(t*databits[0].size()+j));
                if(errorIdx.contains(t*databits[0].size()+j))
                {
                    System.out.print("\u001B[31m"+databits[t].get(j)+"\u001B[0m");
                }
                else
                {
                    System.out.print(databits[t].get(j));
                }
            }
            System.out.println();
        }
    }

    void reconstruct(ArrayList<Integer> databits[])
    {
        for(int i=0;i<databits.length;i++)
        {
            int idx=0;
            int j=0;
            int mask=1;
            while(mask<databits[i].size())
            {
                int parity=0;
                for(int k=0;k<databits[i].size();k++)
                {
                    if(((k+1)&mask)!=0)
                    {
                        parity^=databits[i].get(k);
                    }
                }
                //find the position of error
                if(parity!=0)
                {
                    idx+=mask;
                }
                j++;
                mask=1<<j;
            }
            //if idx!=0 , then error found
            if(idx!=0 && idx<databits[i].size())
            {
                databits[i].set(idx-1,databits[i].get(idx-1)^1);
            }
        }
    }
    ArrayList<Integer>[] removeCheckBits(ArrayList<Integer> databits[])
    {
        ArrayList<Integer>temp[]=new ArrayList[databits.length];
        for(int i=0;i<databits.length;i++)
            temp[i]=new ArrayList<>();
        
        for(int i=0;i<databits.length;i++)
        {
            for(int j=0;j<databits[i].size();j++)
            {
                if(!isPowerOfTwo(j+1))
                {
                    temp[i].add(databits[i].get(j));
                }
            }
        }
        return temp;
    }

    String getMessage(ArrayList<Integer> databits[])
    {
        //databits to ascii
        String str="";
        for(int i=0;i<databits.length;i++)
        {
            for(int j=0;j<databits[i].size();j+=8)
            {
                int asciiCode=0;
                for(int k=0;k<8;k++)
                {
                    asciiCode+=databits[i].get(j+k)*Math.pow(2,7-k);
                }
                str+=(char)asciiCode;
            }
        }
        return str;
    }
}
    
   

