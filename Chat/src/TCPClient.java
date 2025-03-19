import java.io.PrintWriter;

public class TCPClient extends ServerSideClient {
    private final PrintWriter printWriter;

    public TCPClient(PrintWriter writer) {
        super("TCP: " + ServerSideClient.counter.getAndIncrement());
        this.printWriter = writer;
    }

    public PrintWriter getPrintWriter() {
        return printWriter;
    }
}
