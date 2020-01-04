/**
 * 
 * @author Michael De Angelis
 * @matricola: 560049
 * @project Word Quizzle
 * @A.A 2019 - 2020 [UNIPI]
 *
 */

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.awt.event.ActionEvent;
import javax.swing.JPasswordField;
import javax.swing.JPanel;
import java.awt.Font;

public class WqClientGUI {

	private JFrame loginFrame;
	private JTextField usrSignUp;
	private JPasswordField pswSignUp;
	private JPasswordField confirmPswSignUp;
	private JTextField usrLogin;
	private JPasswordField pswLogin;

	// Used to process/interpret requests/responses to/from the server
	private JsonHandler jsonHandler;
	
	private SocketChannel clientSocket;
	private DatagramSocket clientUdpSocket;
	
	private String usr = null;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WqClientGUI window = new WqClientGUI();
					window.loginFrame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public WqClientGUI() {
		jsonHandler = new JsonHandler();
		
		initialize();
	}
	
	private boolean sendRequest(String request) {
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		length.putInt(request.length());
		length.flip();
		ByteBuffer req = ByteBuffer.wrap(request.getBytes());
		
		try {
			while(length.hasRemaining())
				clientSocket.write(length);
			while(req.hasRemaining())
				clientSocket.write(req);
		} catch (IOException ioe) {
			ErrorMacro.clientIoExceptionHandling(ioe, clientSocket, clientUdpSocket, usr);
			return false;
		}
		
		return true;
	}
	
	private Map<String, String> receiveResponse() {
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		ByteBuffer rsp = ByteBuffer.allocate(MyUtilities.BUFFER_SIZE);
		
		try {
			while(length.hasRemaining())
				clientSocket.read(length);
			length.flip();
			int len = length.getInt();
			while(rsp.position() != len)
				clientSocket.read(rsp);
			rsp.flip();
		} catch (IOException ioe) {
			ErrorMacro.clientIoExceptionHandling(ioe, clientSocket, clientUdpSocket, usr);
			return null;
		}
		
		return jsonHandler.fromJson(new String(rsp.array()).trim());
	}
	
	private void exit() {
		try {
		if(clientSocket != null)
			clientSocket.close();
		if(clientUdpSocket != null)
			clientUdpSocket.close();
		if(usr != null)
			usr = null;
		} catch (IOException ioe) {
			ErrorMacro.softIoExceptionHandling(ioe);
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		loginFrame = new JFrame();
		loginFrame.setBounds(100, 100, 450, 300);
		loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		loginFrame.getContentPane().setLayout(null);
		
		JPanel rootJPanel = new JPanel();
		rootJPanel.setBounds(0, 0, 434, 261);
		loginFrame.getContentPane().add(rootJPanel);
		rootJPanel.setLayout(null);
		
		JPanel usrOp = new JPanel();
		usrOp.setBounds(0, 0, 10, 10);
		loginFrame.getContentPane().add(usrOp);
		
		JLabel lblNewLabel_1 = new JLabel("Michael De Angelis 560049 2019 - 2020");
		lblNewLabel_1.setFont(new Font("Tahoma", Font.PLAIN, 9));
		lblNewLabel_1.setBounds(0, 0, 373, 12);
		rootJPanel.add(lblNewLabel_1);

		// Sign Up section
		
		JLabel lblUsername = new JLabel("Username");
		lblUsername.setBounds(31, 27, 86, 14);
		rootJPanel.add(lblUsername);
		
		JLabel lblNewLabel = new JLabel("Password");
		lblNewLabel.setBounds(31, 87, 86, 14);
		rootJPanel.add(lblNewLabel);
		
		JLabel lblConfirmPassword = new JLabel("Confirm Password");
		lblConfirmPassword.setBounds(31, 143, 141, 14);
		rootJPanel.add(lblConfirmPassword);

		usrSignUp = new JTextField();
		usrSignUp.setBounds(31, 52, 141, 20);
		rootJPanel.add(usrSignUp);
		usrSignUp.setColumns(10);

		pswSignUp = new JPasswordField();
		pswSignUp.setBounds(31, 112, 141, 20);
		rootJPanel.add(pswSignUp);
		pswSignUp.setColumns(10);
		
		confirmPswSignUp = new JPasswordField();
		confirmPswSignUp.setBounds(31, 168, 141, 20);
		rootJPanel.add(confirmPswSignUp);
		confirmPswSignUp.setColumns(10);
		
		// Handle the registration to the WQ Server
		JButton signUpButton = new JButton("Sign Up");
		signUpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {		// Password confirmation
				if(!Arrays.equals(pswSignUp.getPassword(), confirmPswSignUp.getPassword())) {
					JOptionPane.showMessageDialog(loginFrame, MyUtilities.USR_DIFFERENT_PSW);
					return;
				}
				
				try {
					Registry reg = LocateRegistry.getRegistry(MyUtilities.SIGN_UP_PORT);
					WqSignUp stub = (WqSignUp)reg.lookup(MyUtilities.SERVER_SIGN_UP_NAME);
					
					// Notify the response received by the server
					Map<String, String> retval = 
							jsonHandler.fromJson(stub.signUp(usrSignUp.getText(), String.valueOf(pswSignUp.getPassword())));
					
					if(retval.containsKey(MyUtilities.CLIENT_ERROR_CODE))
						JOptionPane.showMessageDialog(loginFrame, MyUtilities.ERR_ENTERING_PARS + retval.get(MyUtilities.CLIENT_ERROR_CODE));
					else if(retval.containsKey(MyUtilities.RETRY_WITH_CODE))
						JOptionPane.showMessageDialog(loginFrame, MyUtilities.URS_TAKEN + retval.get(MyUtilities.RETRY_WITH_CODE));
					else if(retval.containsKey(MyUtilities.SUCCESS_CODE)) {
						JOptionPane.showMessageDialog(loginFrame, MyUtilities.SUCCESSFUL_REG + retval.get(MyUtilities.SUCCESS_CODE));
						
						// Clean the fields so the client can perform another registration without have to delete the previous information itself
						usrSignUp.setText(null);
						pswSignUp.setText(null);
						confirmPswSignUp.setText(null);
					}
				} catch (RemoteException | NotBoundException re_nbe) {
					JOptionPane.showMessageDialog(loginFrame, MyUtilities.CONNECTION_FAIL);
				}
			}
		});
		signUpButton.setBounds(31, 207, 86, 31);
		rootJPanel.add(signUpButton);
		
		// Login section
		
		JLabel lblUsername_1 = new JLabel("Username");
		lblUsername_1.setBounds(268, 55, 86, 14);
		rootJPanel.add(lblUsername_1);
		
		JLabel lblPassword = new JLabel("Password");
		lblPassword.setBounds(268, 115, 86, 14);
		rootJPanel.add(lblPassword);

		usrLogin = new JTextField();
		usrLogin.setBounds(268, 84, 141, 20);
		rootJPanel.add(usrLogin);
		usrLogin.setColumns(10);
		
		pswLogin = new JPasswordField();
		pswLogin.setBounds(268, 140, 141, 20);
		rootJPanel.add(pswLogin);
		pswLogin.setColumns(10);
		
		JButton btnLogin = new JButton("Login");
		btnLogin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {		// Open the connection with the Server and the 
					clientSocket = SocketChannel.open(new InetSocketAddress(InetAddress.getLoopbackAddress(), MyUtilities.TCP_CONTROL_PORT));
					clientUdpSocket = new DatagramSocket();
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(loginFrame, MyUtilities.CONNECTION_FAIL);
					return;
				}
				
				// Create login request, send the request, receive the response
				String loginRequest = jsonHandler.toJson(Collections.singletonMap(MyUtilities.LOGIN, 
						usrLogin.getText() + MyUtilities.SPLIT + String.valueOf(pswLogin.getPassword())
						+ MyUtilities.SPLIT + String.valueOf(clientUdpSocket.getLocalPort())));
				
				if(!sendRequest(loginRequest)) {
					JOptionPane.showMessageDialog(loginFrame, MyUtilities.LOGIN_ERR_SEND_REQ);
					return;
				}
				
				Map<String, String> retval = receiveResponse();
				if(retval == null) {
					JOptionPane.showMessageDialog(loginFrame, MyUtilities.LOGIN_ERR_SEND_REQ);
					return;
				}
				
				usr = usrLogin.getText();
				
				if(retval.containsKey(MyUtilities.CLIENT_ERROR_CODE)) {
					JOptionPane.showMessageDialog(loginFrame, MyUtilities.ERR_ENTERING_PARS + retval.get(MyUtilities.CLIENT_ERROR_CODE));
					exit();
				}
				else if(retval.containsKey(MyUtilities.UNAUTHORIZED_CODE)) {
					JOptionPane.showMessageDialog(loginFrame, MyUtilities.CLIENT_ALREADY_ON);
					exit();
				}
				else if(retval.containsKey(MyUtilities.SUCCESS_CODE)) {
					JOptionPane.showMessageDialog(loginFrame, MyUtilities.CLIENT_ONLINE + retval.get(MyUtilities.SUCCESS_CODE));
					
					usrLogin.setText(null);
					pswLogin.setText(null);
					
					
				}
			}
		});
		btnLogin.setBounds(268, 181, 76, 31);
		rootJPanel.add(btnLogin);
	}
}
