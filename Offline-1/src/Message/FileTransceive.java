package Message;

import java.io.Serializable;

public class FileTransceive implements Serializable
{
    public byte[] buffer;
    public int chunk_size;
    public int bytes_read,total_size;
    public String name,status,from,to; //filename, ,uploader,requester

    public FileTransceive(String size)
    {
        chunk_size=Integer.valueOf(size);
        buffer=new byte[chunk_size];
    }

    public FileTransceive(byte[] buffer, int chunk_size, int bytes_read,String n,String s,String f,int len,String to) {
        this.buffer = buffer;
        this.chunk_size = chunk_size;
        this.bytes_read = bytes_read;
        name=n;
        status=s;
        from=f;
        total_size=len;
        this.to=to;
    }

    public String toString()
    {
        return  name;
    }

    @Override
    public boolean equals(Object obj) {
        FileTransceive ft=(FileTransceive) obj;
        if(ft.name.equalsIgnoreCase(((FileTransceive) obj).name))
            return true;
        return false;
    }
}
