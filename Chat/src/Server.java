import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final List<ServerSideClient> clients = Collections.synchronizedList(new ArrayList<>());
    private static final int PORT = 12345;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        System.out.println("SERVER STARTED");
        startUdpServer();
        startTcpServer();
    }

    private static void startUdpServer() {
        Thread udpThread = new Thread(() -> {
            try (DatagramSocket udpSocket = new DatagramSocket(PORT)) {
                byte[] receiveBuffer = new byte[1024];

                while (true) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    udpSocket.receive(receivePacket);

                    InetSocketAddress senderAddress = new InetSocketAddress(receivePacket.getAddress(), receivePacket.getPort());
                    UDPClient udpSender = registerUdpClient(senderAddress);

                    System.out.println("Received UDP message from " + senderAddress);
                    broadcastUdpMessage(receivePacket, udpSender, udpSocket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        udpThread.start();
    }

    private static void startTcpServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New TCP Client Connected.");

                threadPool.submit(() -> handleTcpClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            shutdownServer();
        }
    }

    private static void handleTcpClient(Socket clientSocket) {
        ServerSideClient currentClient = null;
        try (
                Socket autoClosableSocket = clientSocket;
                PrintWriter out = new PrintWriter(autoClosableSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(autoClosableSocket.getInputStream()))
        ) {
            currentClient = new TCPClient(out);
            clients.add(currentClient);

            String msg;
            while ((msg = in.readLine()) != null) {
                if ("exit".equalsIgnoreCase(msg.trim())) {
                    System.out.println("Client " + currentClient.getId() + " has signaled disconnection.");
                    break;
                }
                System.out.println("Received TCP message from " + currentClient.getId() + " -> " + msg);
                broadcastTcpMessage(msg, currentClient);
            }
            System.out.println("Client " + currentClient.getId() + " disconnected gracefully.");
        } catch (SocketException exception) {
            System.out.println("Client " + Objects.requireNonNull(currentClient).getId() + " has disconnected.");
        } catch (IOException exception) {
            exception.printStackTrace();
        } finally {
            if (currentClient != null) {
                clients.remove(currentClient);
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static UDPClient registerUdpClient(InetSocketAddress senderAddress) {
        return clients.stream()
                .filter(client -> client instanceof UDPClient)
                .map(client -> (UDPClient) client)
                .filter(client -> client.getSocketAddress().equals(senderAddress))
                .findFirst()
                .orElseGet(() -> {
                    UDPClient newClient = new UDPClient(senderAddress);
                    clients.add(newClient);
                    System.out.println("New UDP Client Registered: " + senderAddress);
                    return newClient;
                });
    }

    private static void broadcastUdpMessage(DatagramPacket packet, UDPClient sender, DatagramSocket udpSocket) {
        clients.stream()
                .filter(client -> client instanceof UDPClient)
                .map(client -> (UDPClient) client)
                .filter(udpClient -> !udpClient.equals(sender))
                .forEach(udpClient -> {
                    DatagramPacket forwardPacket = new DatagramPacket(
                            packet.getData(),
                            packet.getLength(),
                            udpClient.getSocketAddress().getAddress(),
                            udpClient.getSocketAddress().getPort()
                    );
                    try {
                        udpSocket.send(forwardPacket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private static void broadcastTcpMessage(String message, ServerSideClient sender) {
        String namedMessage = sender.getId() + " -> " + message;
        synchronized (clients) {
            clients.stream()
                    .filter(client -> client instanceof TCPClient)
                    .map(client -> (TCPClient) client)
                    .filter(tcpClient -> !tcpClient.equals(sender))
                    .forEach(tcpClient -> tcpClient.getPrintWriter().println(namedMessage));
        }
    }


    private static void shutdownServer() {
        threadPool.shutdown();
        clients.removeIf(client -> {
            if (client instanceof TCPClient tcpClient) {
                return tcpClient.getPrintWriter().checkError();
            }
            return false;
        });
    }
}
