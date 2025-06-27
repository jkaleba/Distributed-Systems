module LoadBalancer {
    interface Echo {
        string echoString(string msg);
    }
    interface Calculator {
        int add(int a, int b);
        int multiply(int a, int b);
    }
};
