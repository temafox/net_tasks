package chat.client;

import network.TCPConnection;
import network.TCPConnectionListener;
import network.TransactionCode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class ClientWindow extends JFrame implements ActionListener, TCPConnectionListener {
    private boolean authorized = false;

    private static final String IP_ADDR = "172.20.10.8";
    private static final int PORT = 8189;
    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClientWindow(); //Таким образом эта строчка выполнится в потоке EDT
            }
        });
    }

    private final JTextArea log = new JTextArea();
    private final JTextField nickName = new JTextField("WHO ARE U");
    private final JTextField addressees = new JTextField();
    private final JTextField fieldInput = new JTextField();

    private TCPConnection connection;

    private ClientWindow() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        log.setEditable(false); //Запрещаем редактировать то, что по середине
        log.setLineWrap(true); //Запретили автоматический перенос слов
        addressees.setEditable(false);
        fieldInput.setEditable(false);
        fieldInput.addActionListener(this);
        nickName.addActionListener(this);
        add(log, BorderLayout.CENTER);
        add(nickName, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new GridLayout(2, 1));
        bottom.add(addressees);
        bottom.add(fieldInput);
        add(bottom, BorderLayout.SOUTH);

        try {
            connection = new TCPConnection(this, IP_ADDR, PORT);
        } catch (IOException e) {
            printMsg("Connection exception: " + e);
        }

        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(!authorized) {
            connection.sendString(TransactionCode.CS_IDENTIFICATION + "\f" + nickName.getText() + "\n");
        } else {
            String message = fieldInput.getText();
            if (message.equals("")) return;
            fieldInput.setText(null);
            connection.sendString( TransactionCode.CS_MESSAGE + "\f"
                    + convertAddressees(addressees.getText()) + "\b" + message + "\n");
        }
    }

    private String convertAddressees(String addressees) {
        return addressees.replaceAll(",", "\f");
    }

    @Override
    public void onConnectionReady(TCPConnection tcpConnection) {
        printMsg("Connection ready...");
    }

    @Override
    public void onReceiveString(TCPConnection tcpConnection, String value) {
        if (value.startsWith(TransactionCode.SC_NOTIFICATION.toString())) {
            printMsg("Server notification: " + value.substring(value.indexOf('\f') + 1));
        } else
            if(!authorized) {
                String serverResponse = value;
                if (serverResponse.startsWith(TransactionCode.SC_CONFIRMATION.toString())) {
                    authorized = true;
                    nickName.setEditable(false);
                    addressees.setEditable(true);
                    fieldInput.setEditable(true);
                } else if (serverResponse.startsWith(TransactionCode.SC_REAUTHORIZATION.toString())) {
                    printMsg(serverResponse.substring(serverResponse.indexOf('\f') + 1));
                } else
                    printMsg("Incorrect server response: " + value);
            } else {
                if (value.startsWith(TransactionCode.SC_MESSAGE.toString())) {
                    String str = "Message from " + value.substring(value.indexOf('\f') + 1, value.lastIndexOf('\f')) + ": ";
                    str += value.substring(value.lastIndexOf('\f') + 1);
                    printMsg(str);
                } else
                    printMsg("Unrecognized server message: " + value);
            }
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {
        printMsg("Connection is closed...");
    }

    @Override
    public void onException(TCPConnection tcpConnection, Exception e) {
        printMsg("System exception: " + e);
    }

    private synchronized void printMsg(String msg) { //Нам нужен ЕДТ, т. к. Swing работает со спецпотоком
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append(msg + "\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
        });
    }
}
