package Message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Respond implements Serializable
{
    String respond;
    public String desc,to;
    List<Object> list;

    public Respond(String r)
    {
        respond=r;
        list=new ArrayList<>();
    }

    public String getRespond()
    {
        return respond;
    }

    public void setList(List<Object> l)
    {
        list=l;
    }

    public void add(Object o)
    {
        list.add(o);
    }

    public List<Object> getList() {
        return list;
    }
}
