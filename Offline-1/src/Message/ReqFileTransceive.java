package Message;

public class ReqFileTransceive extends FileTransceive{


    public String des; //if requested file, then a description is a must
    public ReqFileTransceive(String size, String des)
    {
        super(size);
        this.des=des;
    }
}
