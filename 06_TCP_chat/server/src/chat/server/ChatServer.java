package chat.server;

import network.TCPConnection;
import network.TCPConnectionListener;
import network.TransactionCode;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

public class ChatServer implements TCPConnectionListener {
    public static void main(String[] args) {
        new ChatServer();
    }

    private final ArrayList<TCPConnection> connections = new ArrayList<>();

    private ChatServer() {
        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            while (true) {
                try {
                    new TCPConnection(this, serverSocket.accept());
                    // объект сокета при входящем сокете, как только соединение установилось
                    // этот метод ждет метод соединения, передает в конструктор TCPConnection и создает экземпляр
                } catch (IOException e) {
                    System.out.println("TCPConnection exception: " + e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void onConnectionReady(TCPConnection tcpConnection) {
        connections.add(tcpConnection);
    }

    @Override
    public synchronized void onReceiveString(TCPConnection tcpConnection, String value) {//Если приняли строчку разослать всем
        if (tcpConnection.nickname == null) {
            String authRequest = value;
            if (authRequest.startsWith(TransactionCode.CS_IDENTIFICATION.toString())) {
                String newNickname = authRequest.substring(authRequest.indexOf('\f') + 1);
                for (TCPConnection connection : connections) {
                    if (newNickname.equals(connection.nickname)) {
                        tcpConnection.sendString(TransactionCode.SC_REAUTHORIZATION + "\f"
                                + newNickname + " already exists. Try again\n");
                        return;
                    }
                }
                // никнейм прошёл проверку
                tcpConnection.nickname = newNickname;
                tcpConnection.sendString(TransactionCode.SC_CONFIRMATION + "\n");
                sendToAllConnections(TransactionCode.SC_NOTIFICATION + "\f"
                        + tcpConnection.nickname + " has connected\n");
            } else
                tcpConnection.sendString(TransactionCode.SC_REAUTHORIZATION + "\fIncorrect request\n");
        } else {
            String clientRequest = value;
            if(clientRequest.startsWith(TransactionCode.CS_MESSAGE.toString())) {
                int firstAddresseePosition = clientRequest.indexOf('\f') + 1;
                int messageStartPosition = clientRequest.indexOf('\b') + 1;
                String message = clientRequest.substring(messageStartPosition);

                if(firstAddresseePosition + 1 == messageStartPosition) { // nobody listed as an addressee
                    sendToAllConnections(TransactionCode.SC_MESSAGE + "\f" + tcpConnection.nickname + "\f" + message + "\n");
                } else { // there are specified addressees
                    ArrayList< String > addressees = new ArrayList<>();
                    String beingParsed = clientRequest.substring(firstAddresseePosition, messageStartPosition - 1);
                    StringBuilder currentGuy = new StringBuilder();

                    addressees.add(tcpConnection.nickname); // send his message back to him
                    for (int i = 0; i < beingParsed.length(); i++) {
                        if(beingParsed.charAt(i) == '\f' || i == beingParsed.length() - 1) {
                            addressees.add(currentGuy.toString());
                            currentGuy = new StringBuilder();
                        } else {
                            currentGuy.append(beingParsed.charAt(i));
                        }
                    }

                    for (TCPConnection guy: connections) {
                        if(addressees.contains(guy.nickname))
                            guy.sendString(TransactionCode.SC_MESSAGE + "\f" + tcpConnection.nickname + "\f" + message + "\n");
                    }
                }
            } else
                tcpConnection.sendString(TransactionCode.SC_NOTIFICATION + "\fwrong request\n");
        }
    }

    @Override
    public synchronized void onDisconnect(TCPConnection tcpConnection) {
        connections.remove(tcpConnection);
        if(tcpConnection.nickname != null)
            sendToAllConnections(TransactionCode.SC_NOTIFICATION + "\f" + tcpConnection.nickname + " has disconnected\n");
    }

    @Override
    public synchronized void onException(TCPConnection tcpConnection, Exception e) {
        System.out.println("TCPConnection exception: " + e);
    }

    private void sendToAllConnections(String value) {
        System.out.println(value);
        for (int i = 0; i < connections.size(); i++)
            connections.get(i).sendString(value);
    }
}
