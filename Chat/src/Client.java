import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    private static final String HOST_NAME = "localhost";
    private static final int TCP_PORT = 12345;
    private static final int UDP_PORT = 12345;
    private static final int MULTICAST_PORT = 12347;
    private static final String MULTICAST_ADDRESS = "230.0.0.0";

    public static void main(String[] args) {
        System.out.println("CLIENT STARTED");

        try (
                Socket socket = new Socket(HOST_NAME, TCP_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Scanner scanner = new Scanner(System.in);
                DatagramSocket udpSocket = new DatagramSocket();
                MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT)
        ) {
            multicastSocket.setReuseAddress(true);

            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            multicastSocket.joinGroup(group);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down... Closing sockets.");
                try {
                    if (out != null) {
                        out.println("exit");
                        out.flush();
                    }
                    if (multicastSocket != null && !multicastSocket.isClosed()) {
                        multicastSocket.leaveGroup(InetAddress.getByName(MULTICAST_ADDRESS));
                        multicastSocket.close();
                    }
                    if (udpSocket != null && !udpSocket.isClosed()) {
                        udpSocket.close();
                    }
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));


            startTcpListener(in);
            startUdpListener(udpSocket);
            startMulticastListener(multicastSocket);

            handleUserInput(scanner, out, udpSocket, multicastSocket, group);

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void startTcpListener(BufferedReader in) {
        Thread tcpListenerThread = new Thread(() -> {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    System.out.println("Received TCP: " + response);
                }
            } catch (IOException e) {
                if (!e.getMessage().equals("Socket closed")) {
                    System.out.println("Error receiving TCP message: " + e.getMessage());
                }
            }
        });
        tcpListenerThread.setDaemon(true);
        tcpListenerThread.start();
    }

    private static void startUdpListener(DatagramSocket udpSocket) {
        Thread udpListenerThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (!udpSocket.isClosed()) {
                DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length);
                try {
                    udpSocket.receive(udpPacket);
                    String udpResponse = new String(udpPacket.getData(), 0, udpPacket.getLength());
                    System.out.println("Received UDP:\n" + udpResponse);
                } catch (IOException e) {
                    if (!udpSocket.isClosed()) {
                        System.out.println("Error receiving UDP message: " + e.getMessage());
                    }
                    break;
                }
            }
        });
        udpListenerThread.setDaemon(true);
        udpListenerThread.start();
    }

    private static void startMulticastListener(MulticastSocket multicastSocket) {
        Thread multicastListenerThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (!multicastSocket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    multicastSocket.receive(packet);
                    String multicastResponse = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Received Multicast:\n" + multicastResponse);
                } catch (IOException e) {
                    if (!multicastSocket.isClosed()) {
                        System.out.println("Error receiving Multicast message: " + e.getMessage());
                    }
                }
            }
        });
        multicastListenerThread.setDaemon(true);
        multicastListenerThread.start();
    }

    private static void handleUserInput(Scanner scanner, PrintWriter out, DatagramSocket udpSocket, MulticastSocket multicastSocket, InetAddress multicastGroup) throws IOException {
        System.out.println("Enter the message, 'U' -> UDP, 'M' -> Multicast.");
        while (scanner.hasNextLine()) {
            String input = scanner.nextLine();
            if ("U".equalsIgnoreCase(input.trim())) {
                sendUdpMessage(udpSocket);
            } else if ("M".equalsIgnoreCase(input.trim())) {
                sendMulticastMessage(multicastSocket, multicastGroup);
            } else {
                out.println(input);
            }
        }
    }

    private static void sendUdpMessage(DatagramSocket udpSocket) throws IOException {
        String asciiArt =
                        "██         ██  \n" +
                        "██         ██  \n" +
                        "██         ██  \n" +
                        "██         ██  \n" +
                        "██         ██  \n" +
                        "██         ██  \n" +
                        "██         ██  \n" +
                        "██         ██  \n" +
                        "██         ██  \n" +
                        " ██       ██   \n" +
                        "  ██     ██    \n" +
                        "   ███████     ";
        byte[] data = asciiArt.getBytes();
        DatagramPacket udpPacket = new DatagramPacket(data, data.length, InetAddress.getByName(HOST_NAME), UDP_PORT);
        udpSocket.send(udpPacket);
        System.out.println("Sent UDP message with ASCII Art.");
    }

    private static void sendMulticastMessage(MulticastSocket multicastSocket, InetAddress multicastGroup) throws IOException {
        String multicastMessage =
                        "███       ███  \n" +
                        "████     ████  \n" +
                        "█████   █████  \n" +
                        "██ ██   ██ ██  \n" +
                        "██  ██ ██  ██  \n" +
                        "██   ███   ██  \n" +
                        "██    █    ██  \n" +
                        "██         ██  \n" +
                        "██         ██  \n" +
                        "██         ██  \n" +
                        "██         ██  \n" +
                        "██         ██  ";
        byte[] data = multicastMessage.getBytes();
        DatagramPacket multicastPacket = new DatagramPacket(data, data.length, multicastGroup, MULTICAST_PORT);
        multicastSocket.send(multicastPacket);
        System.out.println("Sent Multicast message with ASCII Art.");
    }
}
