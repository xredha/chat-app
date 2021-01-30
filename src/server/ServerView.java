package server;

import ui.PrimaryColor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.*;

public class ServerView {
    private static final long serialVersionUID = 1L;

    private static final Map<String, Socket> allUsersList = new ConcurrentHashMap<>();
    private static final Set<String> activeUserSet = new HashSet<>();
    private static JFrame frame;
    private ServerSocket serverSocket;
    private JTextArea serverMessageBoard;
    private JList<String> allUserNameList;
    private JList<String> activeClientList;
    private final DefaultListModel<String> activeUsers = new DefaultListModel<String>();
    private final DefaultListModel<String> allUsers = new DefaultListModel<String>();
    private JTextField serverPort;
    private final JButton btnPortConnect = new JButton("Connect");
    private final JButton btnPortDisconnect = new JButton("Disconnect");

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ServerView window = new ServerView();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public ServerView() {
        initialize();
        // Apabila tombol connect di klik
        btnPortConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String portValue = serverPort.getText();
                    serverSocket = new ServerSocket(Integer.parseInt(portValue));
                    serverMessageBoard.append("Server berada pada port: " + portValue + "\n");
                    serverMessageBoard.append("Menunggu Client...\n");
                    new ClientAccept().start(); // this will create a thread for client
                    // Apabila tombol disconnect di klik
                    btnPortDisconnect.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            new ClientAccept().interrupt();
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    class ClientAccept extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();  // Socket Client
                    String uName = new DataInputStream(clientSocket.getInputStream()).readUTF();
                    DataOutputStream cOutStream = new DataOutputStream(clientSocket.getOutputStream());
                    if (activeUserSet.contains(uName)) {
                        cOutStream.writeUTF("Username sudah digunakan");
                    } else {
                        allUsersList.put(uName, clientSocket); // Tambah ke active dan all users
                        activeUserSet.add(uName);
                        cOutStream.writeUTF(""); // hapus pesan di box
                        activeUsers.addElement(uName);
                        if (!allUsers.contains(uName))
                            allUsers.addElement(uName);
                        activeClientList.setModel(activeUsers);
                        allUserNameList.setModel(allUsers);
                        serverMessageBoard.append("Client " + uName + " Connected...\n");
                        new MsgRead(clientSocket, uName).start(); // create a thread to read messages
                        new PrepareClientStart().start(); //create a thread to update all the active clients
                    }
                } catch (Exception e) {
                    System.out.println("Server has ben disconnect...");
                    break;
                }
            }
        }

        @Override
        public void interrupt() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverMessageBoard.setText("Server has been Disconnect..." + "\n");
        }
    }

    class MsgRead extends Thread { // Class untuk membaca pesan
        Socket s;
        String Id;
        private MsgRead(Socket s, String uname) {
            this.s = s;
            this.Id = uname;
        }

        @Override
        public void run() {
            while (allUserNameList != null && !allUsersList.isEmpty()) {  // Jika allUsersList tidak kosong
                try {
                    String message = new DataInputStream(s.getInputStream()).readUTF();
                    System.out.println("message read ==> " + message); // Print testing CLI
                    String[] msgList = message.split(":"); // identifier
                    if (msgList[0].equalsIgnoreCase("multicast")) {
                        serverMessageBoard.append(msgList[0] + "(" + msgList[1] + ")" + " : " + msgList[2] + "\n"); // Menampilkan chat personal ke server
                        String[] sendToList = msgList[1].split(",");
                        for (String usr : sendToList) { // untuk semua user kirim pesan
                            try {
                                if (activeUserSet.contains(usr)) { // cek user aktif atau tidak
                                    new DataOutputStream(allUsersList.get(usr).getOutputStream())
                                            .writeUTF("< " + Id + " >" + msgList[2]);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (msgList[0].equalsIgnoreCase("broadcast")) { // Broadcast untuk semua user
                        serverMessageBoard.append(msgList[0] + " : " + msgList[1] + "\n"); // Menampilkan chat broadcast ke server
                        Iterator<String> itr1 = allUsersList.keySet().iterator();
                        while (itr1.hasNext()) {
                            String usrName = itr1.next();
                            if (!usrName.equalsIgnoreCase(Id)) { // agar tidak ngirim ke diri sendiri
                                try {
                                    if (activeUserSet.contains(usrName)) { // cek jika client aktif
                                        new DataOutputStream(allUsersList.get(usrName).getOutputStream())
                                                .writeUTF("< " + Id + " >" + msgList[1]);
                                    } else {
                                        // cek jika client tidak aktif
                                        new DataOutputStream(s.getOutputStream())
                                                .writeUTF("Message couldn't be delivered to user " + usrName + " because it is disconnected.");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else if (msgList[0].equalsIgnoreCase("exit")) {  // Jika klik offline
                        activeUserSet.remove(Id); // Hapus dari active users
                        serverMessageBoard.append(Id + " disconnected....\n");

                        new PrepareClientStart().start(); // Update Active User

                        Iterator<String> itr = activeUserSet.iterator();
                        while (itr.hasNext()) {
                            String usrName2 = itr.next();
                            if (!usrName2.equalsIgnoreCase(Id)) { // Agar tidak mengirim ke diri sendiri
                                try {
                                    new DataOutputStream(allUsersList.get(usrName2).getOutputStream())
                                            .writeUTF(Id + " disconnected..."); // Notif jika ada user yang disconnect
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                new PrepareClientStart().start(); // Update user active jika ada yang disconnect
                            }
                        }
                        activeUsers.removeElement(Id); // Hilangkan dari list server
                        activeClientList.setModel(activeUsers); // Update list user
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class PrepareClientStart extends Thread {
        @Override
        public void run() {
            try {
                String ids = "";
                Iterator<String> itr = activeUserSet.iterator();
                while (itr.hasNext()) {
                    String key = itr.next();
                    ids += key + ",";
                }
                if (ids.length() != 0) {
                    ids = ids.substring(0, ids.length() - 1);
                }
                itr = activeUserSet.iterator();
                while (itr.hasNext()) {
                    String key = itr.next();
                    try {
                        new DataOutputStream(allUsersList.get(key).getOutputStream())
                                .writeUTF(":;.,/=" + ids); // Kirim ke aktif user
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 800, 550);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        frame.setTitle("Server View");

        serverPort = new JTextField();
        serverPort.setBounds(514, 53, 136, 30);
        frame.getContentPane().add(serverPort);

        serverMessageBoard = new JTextArea();
        serverMessageBoard.setEditable(false);
        serverMessageBoard.setBounds(27, 60, 406, 446);
        frame.getContentPane().add(serverMessageBoard);
        serverMessageBoard.setText("Memulai Server...\n");

        activeClientList = new JList<>();
        activeClientList.setBounds(460, 135, 318, 160);
        frame.getContentPane().add(activeClientList);

        allUserNameList = new JList<String>();
        allUserNameList.setBounds(460, 346, 318, 160);
        frame.getContentPane().add(allUserNameList);

        JLabel lblNameGroup = new JLabel("PBO A Kelompok 7");
        lblNameGroup.setFont(new Font("Tahoma", Font.PLAIN, 18));
        lblNameGroup.setHorizontalAlignment(SwingConstants.CENTER);
        lblNameGroup.setBounds(310, 17, 180, 21);
        frame.getContentPane().add(lblNameGroup);

        JLabel lblPort = new JLabel("Port");
        lblPort.setHorizontalAlignment(SwingConstants.LEFT);
        lblPort.setBounds(460, 60, 27, 16);
        lblPort.setFont(new Font("Tahoma", Font.PLAIN, 12));
        frame.getContentPane().add(lblPort);

        JLabel lblActiveUsers = new JLabel("Active Users");
        lblActiveUsers.setBounds(460, 102, 120, 16);
        lblActiveUsers.setFont(new Font("Tahoma", Font.PLAIN, 14));
        frame.getContentPane().add(lblActiveUsers);

        JLabel lblAllUsernames = new JLabel("All Usernames");
        lblAllUsernames.setHorizontalAlignment(SwingConstants.LEFT);
        lblAllUsernames.setBounds(460, 313, 120, 16);
        lblAllUsernames.setFont(new Font("Tahoma", Font.PLAIN, 14));
        frame.getContentPane().add(lblAllUsernames);

        btnPortDisconnect.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnPortDisconnect.setBounds(673, 95, 105, 30);
        btnPortDisconnect.setBackground(PrimaryColor.btnRed);
        btnPortDisconnect.setForeground(Color.white);
        frame.getContentPane().add(btnPortDisconnect);

        btnPortConnect.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnPortConnect.setBounds(673, 53, 105, 30);
        btnPortConnect.setBackground(PrimaryColor.btnBlue);
        btnPortConnect.setForeground(Color.white);
        frame.getContentPane().add(btnPortConnect);
    }
}
