package client;

import ui.PrimaryColor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class ClientView extends JFrame {
    private static final long serialVersionUID = 1L;

    private JFrame frame;
    private JTextField clientTypingBoard;
    private JList<String> clientActiveUsersList;
    private JTextArea clientMessageBoard;
    private JRadioButton oneToNRadioBtn;
    private JRadioButton broadcastBtn;

    DataInputStream inputStream;
    DataOutputStream outStream;
    DefaultListModel<String> dm;
    String id, clientIds = "";

    /**
     * Create the application.
     */

    public ClientView(String id, Socket s) {
        initialize();
        this.id = id;
        try {
            frame.setTitle("Client View - " + id);
            dm = new DefaultListModel<String>(); // Active users
            clientActiveUsersList.setModel(dm);// show that list on UI component JList named clientActiveUsersList
            inputStream = new DataInputStream(s.getInputStream());
            outStream = new DataOutputStream(s.getOutputStream());
            new Read().start(); // Membuat thread baru untuk membaca pesan
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    class Read extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    String m = inputStream.readUTF();
                    System.out.println("inside read thread : " + m);
                    if (m.contains(":;.,/=")) { // prefix(random)
                        m = m.substring(6); // Koma regex untuk users yang aktif
                        dm.clear(); // Menghapus element
                        StringTokenizer st = new StringTokenizer(m, ","); // Split client id dan tambahkan ke dm
                        while (st.hasMoreTokens()) {
                            String u = st.nextToken();
                            if (!id.equals(u)) // Tidak menampilkan diri kita di dalam active users kita
                                dm.addElement(u); // Menambahkan ke active user
                        }
                    } else {
                        clientMessageBoard.append("" + m + "\n");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 800, 560 );
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        frame.setResizable(false);
        frame.setTitle("Client View");

        clientMessageBoard = new JTextArea();
        clientMessageBoard.setEditable(false);
        clientMessageBoard.setBounds(14, 68, 433, 400);
        frame.getContentPane().add(clientMessageBoard);

        clientActiveUsersList = new JList<>();
        clientActiveUsersList.setToolTipText("Users Online");
        clientActiveUsersList.setBounds(467, 113, 314, 355);
        frame.getContentPane().add(clientActiveUsersList);

        clientTypingBoard = new JTextField();
        clientTypingBoard.setHorizontalAlignment(SwingConstants.LEFT);
        clientTypingBoard.setBounds(14, 484, 433, 35);
        frame.getContentPane().add(clientTypingBoard);
        clientTypingBoard.setColumns(10);

        JButton clientSendMsgBtn = new JButton("Send");
        clientSendMsgBtn.setBounds(467, 484, 137, 35);
        clientSendMsgBtn.setFont(new Font("Tahoma", Font.PLAIN, 14));
        clientSendMsgBtn.setBackground(PrimaryColor.btnBlue);
        clientSendMsgBtn.setForeground(Color.white);
        frame.getContentPane().add(clientSendMsgBtn);

        JButton clientKillProcessBtn = new JButton("Offline");
        clientKillProcessBtn.setBounds(644, 484, 137, 35);
        clientKillProcessBtn.setFont(new Font("Tahoma", Font.PLAIN, 14));
        clientKillProcessBtn.setBackground(PrimaryColor.btnRed);
        clientKillProcessBtn.setForeground(Color.white);
        frame.getContentPane().add(clientKillProcessBtn);

        JLabel lblNameGroup = new JLabel("PBO A Kelompok 7");
        lblNameGroup.setFont(new Font("Tahoma", Font.PLAIN, 18));
        lblNameGroup.setHorizontalAlignment(SwingConstants.CENTER);
        lblNameGroup.setBounds(300, 20, 200, 20);
        frame.getContentPane().add(lblNameGroup);

        JLabel lblUsersOnline = new JLabel("Users Online");
        lblUsersOnline.setHorizontalAlignment(SwingConstants.LEFT);
        lblUsersOnline.setFont(new Font("Tahoma", Font.PLAIN, 14));
        lblUsersOnline.setBounds(467, 90, 100, 16);
        frame.getContentPane().add(lblUsersOnline);

        oneToNRadioBtn = new JRadioButton("To Personal Chat");
        oneToNRadioBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clientActiveUsersList.setEnabled(true);
            }
        });
        oneToNRadioBtn.setSelected(true);
        oneToNRadioBtn.setBounds(620, 60, 140, 16);
        oneToNRadioBtn.setFont(new Font("Tahoma", Font.PLAIN, 12));
        frame.getContentPane().add(oneToNRadioBtn);

        broadcastBtn = new JRadioButton("To All Chat");
        broadcastBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clientActiveUsersList.setEnabled(false);
            }
        });
        broadcastBtn.setBounds(620, 87, 140, 16);
        broadcastBtn.setFont(new Font("Tahoma", Font.PLAIN, 12));
        frame.getContentPane().add(broadcastBtn);

        ButtonGroup btngrp = new ButtonGroup();
        btngrp.add(oneToNRadioBtn);
        btngrp.add(broadcastBtn);

        clientSendMsgBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String textAreaMessage = clientTypingBoard.getText();
                if (textAreaMessage != null && !textAreaMessage.isEmpty()) {  // Cek apakah pesan kosong atau tidak
                    try {
                        String messageToBeSentToServer = "";
                        String cast = "broadcast"; // Untuk penanda
                        int flag = 0; // Penanda radio button
                        if (oneToNRadioBtn.isSelected()) {
                            cast = "multicast";
                            List<String> clientList = clientActiveUsersList.getSelectedValuesList();
                            if (clientList.size() == 0)
                                flag = 1;
                            for (String selectedUsr : clientList) { // Select users
                                if (clientIds.isEmpty())
                                    clientIds += selectedUsr;
                                else
                                    clientIds += "," + selectedUsr;
                            }
                            messageToBeSentToServer = cast + ":" + clientIds + ":" + textAreaMessage; // Kririm pesan ke server
                        } else {
                            messageToBeSentToServer = cast + ":" + textAreaMessage; // Kasus dimana tidak tau id dari user yang dipilih
                        }
                        if (cast.equalsIgnoreCase("multicast")) {
                            if (flag == 1) { // Jika tidak ada user yang ke select
                                JOptionPane.showMessageDialog(frame, "No user selected");
                            } else {
                                outStream.writeUTF(messageToBeSentToServer);
                                clientTypingBoard.setText("");
                                clientMessageBoard.append("< You sent msg to " + clientIds + ">" + textAreaMessage + "\n"); // Menampilkan pesan ke penerima
                            }
                        } else {
                            outStream.writeUTF(messageToBeSentToServer);
                            clientTypingBoard.setText("");
                            clientMessageBoard.append("< You sent msg to All >" + textAreaMessage + "\n");
                        }
                        clientIds = "";
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "User does not exist anymore.");
                    }
                }
            }
        });

        clientKillProcessBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    outStream.writeUTF("exit");
                    clientMessageBoard.append("You are disconnected now.\n");
                    frame.dispose(); // close the frame
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        frame.setVisible(true);
    }
}
