package org.example;

import LoadBalancer.Calculator;
import com.zeroc.Ice.Current;

public class CalculatorI implements Calculator {
    private final String serverId;

    public CalculatorI() {
        this.serverId = System.getProperty("Ice.ProgramName", "brak_id");
    }

    @Override
    public int add(int a, int b, Current current) {
        System.out.println("[Calculator] " + serverId + " obsługuje: " + a + " + " + b);
        return a + b;
    }

    @Override
    public int multiply(int a, int b, Current current) {
        System.out.println("[Calculator] " + serverId + " obsługuje: " + a + " * " + b);
        return a * b;
    }
}
