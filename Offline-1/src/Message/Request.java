package Message;


import Client.*;

import java.io.Serializable;

public class Request implements Serializable
{
    String from, other; //from,to
    public String request,desc;

    public Request(String r)
    {
        request=r;
    }

    public void setRequest(String r)
    {
        request=r;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getRequest() {
        return request;
    }

    public String getFrom() {
        return from;
    }
    public void setTo(String s)
    {
        other=s;
    }
    public String getTo()
    {
        return other;
    }
}
