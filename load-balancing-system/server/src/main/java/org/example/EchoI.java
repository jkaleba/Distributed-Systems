package org.example;

import LoadBalancer.Echo;
import com.zeroc.Ice.Current;

public class EchoI implements Echo {
    private final String serverId;

    public EchoI() {
        this.serverId = System.getProperty("Ice.ProgramName", "brak_id");
    }

    @Override
    public String echoString(String msg, Current current) {
        System.out.println("[Echo] " + serverId + " obsługuje: " + msg);
        return "Echo: " + msg + " [serwer: " + serverId + "]";
    }
}
