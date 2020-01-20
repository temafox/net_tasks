package network;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TCPConnection {
    private final Socket socket; // сокет, который связан
    private final Thread rxThread; // поток, который слушает входящие сообщения
    private final BufferedReader in;
    private final BufferedWriter out;
    private final TCPConnectionListener eventListener; // слушатель событий
    public String nickname = null;

    public TCPConnection(TCPConnectionListener eventListener, String ipAddr, int port) throws IOException {
        this(eventListener, new Socket(ipAddr, port)); // из первого конструктора вызвали второй
    }

    public TCPConnection(TCPConnectionListener eventListener, Socket socket) throws IOException // сокет создали снаружи
    {
        this.eventListener = eventListener;
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        rxThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    eventListener.onConnectionReady(TCPConnection.this);
                    while (!rxThread.isInterrupted()) {
                        eventListener.onReceiveString(TCPConnection.this, in.readLine());
                    }
                } catch (IOException e) {
                    eventListener.onException(TCPConnection.this, e);
                } finally {
                    eventListener.onDisconnect(TCPConnection.this);
                }
            }
        });
        rxThread.start();
    }

    public synchronized void sendString(String value) // синхронизируем, чтобы безопасно обращаться к разным потокам
    {
        try {
            out.write(value);
            out.flush(); // сбрасываем буфер и отправляем его в сеть
        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e);
            disconnect(); // не смогли передать строчку, отсоединяемся
        }
    }

    public synchronized String receiveString() {
        try {
            return in.readLine();
        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e);
            disconnect();
            return null;
        }
    }

    private synchronized void disconnect() {
        rxThread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e);
        }
    }

    @Override
    public String toString() {
        return "TCPConnection:" + socket.getInetAddress() + ": " + socket.getPort();
    }
}
