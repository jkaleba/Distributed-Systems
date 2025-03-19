import java.util.concurrent.atomic.AtomicInteger;

public class ServerSideClient {
    protected static final AtomicInteger counter = new AtomicInteger(1);
    protected final String id;

    public ServerSideClient(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ServerSideClient other = (ServerSideClient) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
