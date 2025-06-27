package org.example;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;

public class Main {
    public static void main(String[] args) {

        try (Communicator communicator = Util.initialize(args)) {
            ObjectAdapter adapter = communicator.createObjectAdapter("Adapter");

            adapter.add(new EchoI(), Util.stringToIdentity("echo"));
            adapter.add(new CalculatorI(), Util.stringToIdentity("calculator"));
            adapter.activate();

            System.out.println("Server started...");
            communicator.waitForShutdown();

        } catch (Exception e) {
            System.err.println("Exception in server:");
            e.printStackTrace();
        }
    }
}
