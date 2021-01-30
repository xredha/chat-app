package client;

import ui.PrimaryColor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class LoginClient extends JFrame{
    private JFrame frame;
    private JTextField clientUsername;
    private JTextField clientPort;
    private JTextField clientHostname;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    LoginClient window = new LoginClient();
                    window.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public LoginClient() {
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        // Membuat Tampilan Aplikasi
        frame = new JFrame();
        frame.setBounds(100, 100, 600, 350);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setTitle("Client Register");

        // Membuat Text Field Hostname
        clientHostname = new JTextField();
        clientHostname.setBounds(136, 70, 329, 30);
        frame.getContentPane().add(clientHostname);

        // Membuat Text Field Port
        clientPort = new JTextField();
        clientPort.setBounds(136, 139, 329, 30);
        frame.getContentPane().add(clientPort);

        // Membuat Text Field Username
        clientUsername = new JTextField();
        clientUsername.setBounds(136, 207, 329, 30);
        frame.getContentPane().add(clientUsername);

        /*
         * Styling Component
         */

        // Button Connect
        JButton clientLoginBtn = new JButton("Connect");
        clientLoginBtn.setFont(new Font("Tahoma", Font.PLAIN, 17));
        clientLoginBtn.setBounds(225, 255, 150, 45);
        clientLoginBtn.setBackground(PrimaryColor.btnBlue);
        clientLoginBtn.setForeground(Color.WHITE);
        frame.getContentPane().add(clientLoginBtn);

        // Nama Kelompok
        JLabel lblNameGroup = new JLabel("PBO A Kelompok 7");
        lblNameGroup.setFont(new Font("Tahoma", Font.PLAIN, 18));
        lblNameGroup.setHorizontalAlignment(SwingConstants.CENTER);
        lblNameGroup.setBounds(210, 14, 185, 20);
        frame.getContentPane().add(lblNameGroup);

        // Label Hostname
        JLabel lblHostname = new JLabel("Hostname");
        lblHostname.setFont(new Font("Tahoma", Font.PLAIN, 14));
        lblHostname.setBounds(136, 47, 100, 16);
        lblHostname.setForeground(PrimaryColor.textGrey);
        frame.getContentPane().add(lblHostname);

        // Label Port
        JLabel lblPort = new JLabel("Port");
        lblPort.setFont(new Font("Tahoma", Font.PLAIN, 14));
        lblPort.setBounds(136, 112, 100, 16);
        lblPort.setForeground(PrimaryColor.textGrey);
        frame.getContentPane().add(lblPort);

        // Label Username
        JLabel lblUsername = new JLabel("Username");
        lblUsername.setFont(new Font("Tahoma", Font.PLAIN, 14));
        lblUsername.setBounds(136, 181, 100, 16);
        lblUsername.setForeground(PrimaryColor.textGrey);
        frame.getContentPane().add(lblUsername);

        clientLoginBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String hostnameValue = clientHostname.getText();
                    String portValue = clientPort.getText();
                    String nameValue = clientUsername.getText();
                    if (hostnameValue.isEmpty() || portValue.isEmpty() || nameValue.isEmpty()) {
                        JOptionPane.showMessageDialog(frame,  "Semua Field Harus Diisi\n");
                    } else {
                        Socket s = new Socket(hostnameValue, Integer.parseInt(portValue)); // Membuat Socket
                        DataInputStream inputStream = new DataInputStream(s.getInputStream());
                        DataOutputStream outStream = new DataOutputStream(s.getOutputStream());
                        outStream.writeUTF(nameValue); // Mengirim ke output stream

                        String msgFromServer = new DataInputStream(s.getInputStream()).readUTF();
                        if (msgFromServer.equals("Username sudah digunakan")) {
                            JOptionPane.showMessageDialog(frame,  "Username Sudah Digunakan\n");
                        } else {
                            new ClientView(nameValue, s); // Membuat ClientView dan Menutup LoginClient
                            frame.dispose();
                        }
                    }
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
