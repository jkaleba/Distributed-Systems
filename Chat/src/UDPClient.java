import java.net.InetSocketAddress;

public class UDPClient extends ServerSideClient {

    private final InetSocketAddress socketAddress;

    public UDPClient(InetSocketAddress socketAddress) {
        super("UDP: " + ServerSideClient.counter.getAndIncrement());
        this.socketAddress = socketAddress;
    }

    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        UDPClient other = (UDPClient) obj;
        return socketAddress.equals(other.socketAddress);
    }
}
