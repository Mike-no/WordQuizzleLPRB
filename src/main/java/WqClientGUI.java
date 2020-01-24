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
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.awt.event.ActionEvent;
import javax.swing.JPasswordField;
import javax.swing.JPanel;
import java.awt.Font;
import java.awt.CardLayout;
import javax.swing.JSeparator;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.swing.JSplitPane;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTextPane;

public class WqClientGUI {	
	private JFrame contentPane;
	private JTextField usrSignUp;
	private JPasswordField pswSignUp;
	private JPasswordField confirmPswSignUp;
	private JTextField usrLogin;
	private JPasswordField pswLogin;
	private JTextField userSearch;
	private JTextField translatedWord;
	private JButton signUpButton;
	private JButton btnShowScore;
	private JButton btnRefresh;
	private JButton btnLogin;
	private JButton btnLogout;
	private JButton btnAddFriend;
	private JButton btnRanking;
	private JButton btnSend;
	private static final String[] options = { MyUtilities.MATCH_LIST, MyUtilities.SCORE_LIST, MyUtilities.CANCEL };

	// Used to process/interpret requests/responses to/from the server
	private JsonHandler jsonHandler;
	private String usr = null;
	
	private SocketChannel clientSocket;
	private DatagramSocket clientUdpSocket;
	private AtomicBoolean safeExit;
	private AtomicBoolean busy;
	
	private boolean canWrite;
	private boolean stop;
	private Lock lock;
	private Condition cond;
	
	// Launch the application.
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					WqClientGUI window = new WqClientGUI();
					window.contentPane.setTitle(MyUtilities.WORD_QUIZZLE);
					window.contentPane.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	// Create the application.
	public WqClientGUI() {
		jsonHandler = new JsonHandler();
		safeExit = new AtomicBoolean(false);
		busy = new AtomicBoolean(false);
		
		initialize();
	}
	
	// Exit procedure for the client side
	private void exit() {
		try {
			safeExit.set(true);
			if(clientSocket != null)
				clientSocket.close();
			if(clientUdpSocket != null)
				clientUdpSocket.close();
			if(usr != null)
				usr = null;
			busy.set(false);
		} catch (IOException ioe) {
			ErrorMacro.softIoExceptionHandling(ioe);
		}
	}

	// Simply use the socket channel for send requests/traductions to the server
	private boolean send(String request) {
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		length.putInt(request.length());
		length.flip();
		ByteBuffer req = ByteBuffer.wrap(request.getBytes());
		
		try {
			while(length.hasRemaining())			// Write all
				clientSocket.write(length);
			while(req.hasRemaining())
				clientSocket.write(req);
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(contentPane, MyUtilities.COMM_ERROR);
			ErrorMacro.clientIoExceptionHandling(ioe, clientSocket, clientUdpSocket, usr, safeExit, busy);
			return false;
		}
		
		return true;
	}
	
	// Simply use the socket channel for receive server responses
	private Map<String, String> receive() {
		ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
		ByteBuffer rsp = ByteBuffer.allocate(MyUtilities.BUFFER_SIZE);
		
		try {
			while(length.hasRemaining()) {			// Read all
				long nread = clientSocket.read(length);
				if(nread < 0) {						// Disconnection
					JOptionPane.showMessageDialog(contentPane, MyUtilities.CONNECTION_CLOSED);
					exit();
					return null;
				}
			}
			length.flip();
			int len = length.getInt();
			while(rsp.position() != len) {
				long nread = clientSocket.read(rsp);
				if(nread < 0) {
					JOptionPane.showMessageDialog(contentPane, MyUtilities.CONNECTION_CLOSED);
					exit();
					return null;
				}
			}
			rsp.flip();
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(contentPane, MyUtilities.COMM_ERROR);
			ErrorMacro.clientIoExceptionHandling(ioe, clientSocket, clientUdpSocket, usr, safeExit, busy);
			return null;
		}
		
		return jsonHandler.fromJson(new String(rsp.array()).trim());
	}
	
	// Combine the methods send and receive (see above)
	private Map<String, String> sendAndReceive(String rqst, String errMsg) {
		if(!send(rqst)) {
			JOptionPane.showMessageDialog(contentPane, errMsg);
			return null;
		}
		
		Map<String, String> retval = receive();
		if(retval == null) {
			JOptionPane.showMessageDialog(contentPane, errMsg);
			return null;
		}
		
		return retval;
	}
	
	// Receive the begin advertisement with a timeout for the handling of the udp packet loss and the translation service latency
	private Map<String, String> receiveBegin() {
		try {				// Wrapping the underlying socket to set a read timeout
			clientSocket.socket().setSoTimeout(MyUtilities.TIMEOUT_UDP);
			ReadableByteChannel wrappedChannel =
					Channels.newChannel(clientSocket.socket().getInputStream());
			
			ByteBuffer length = ByteBuffer.allocate(Integer.BYTES);
			ByteBuffer rsp = ByteBuffer.allocate(MyUtilities.BUFFER_SIZE);
			
			while(length.hasRemaining()) {			// Read all
				long nread = wrappedChannel.read(length);
				if(nread < 0) {						// Disconnection
					JOptionPane.showMessageDialog(contentPane, MyUtilities.CONNECTION_CLOSED);
					exit();
					return null;
				}
			}
			length.flip();
			int len = length.getInt();
			while(rsp.position() != len) {
				long nread = wrappedChannel.read(rsp);
				if(nread < 0) {
					JOptionPane.showMessageDialog(contentPane, MyUtilities.CONNECTION_CLOSED);
					exit();
					return null;
				}
			}
			rsp.flip();
			
			// Reset the timeout ( 0 = infinite timeout )
			clientSocket.socket().setSoTimeout(0);		
			
			return jsonHandler.fromJson(new String(rsp.array()).trim());
		} catch(SocketTimeoutException ste) {
			try {
				clientSocket.socket().setSoTimeout(0);
			} catch (SocketException se) {					// Handled like a soft IOException for convenience
				ErrorMacro.softIoExceptionHandling(se);
			}
			return Collections.singletonMap(MyUtilities.UNAUTHORIZED_CODE, MyUtilities.BEGIN_NOT_RECEIVED);
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(contentPane, MyUtilities.COMM_ERROR);
			ErrorMacro.clientIoExceptionHandling(ioe, clientSocket, clientUdpSocket, usr, safeExit, busy);
			return null;
		}
	}
	
	// Function called by the palyers to handle the match session 
	private void handleMatch(JTextPane commBox) {
		CardLayout cl = (CardLayout)contentPane.getContentPane().getLayout();
		
		/** Wait for BEGIN; since the challenge acceptance response (sent in udp) can be lost, 
		 * a timer is used to receive Begin's adv; this timer can sometimes also be useful for 
		 * communicating the excessive latency of the translation service that it can sometimes have.
		**/
		//Map<String, String> retval = receive();
		Map<String, String> retval = receiveBegin();
		if(retval == null) {
			JOptionPane.showMessageDialog(contentPane, MyUtilities.MATCH_ERR);
			cl.show(contentPane.getContentPane(), "name_95218890939100");
			return;
		}
		
		if(retval.containsKey(MyUtilities.SERVER_ERROR_CODE))
			JOptionPane.showMessageDialog(contentPane, retval.get(MyUtilities.SERVER_ERROR_CODE));
		else if(retval.containsKey(MyUtilities.UNAUTHORIZED_CODE))
			JOptionPane.showMessageDialog(contentPane, retval.get(MyUtilities.UNAUTHORIZED_CODE));
		else if(retval.containsKey(MyUtilities.SUCCESS_CODE)) {
			commBox.setText(MyUtilities.TRANSLATE_FOLLOWING);
			btnSend.setEnabled(true);
			
			// Initialize the variables for socket write synchronization
			canWrite = false;
			lock = new ReentrantLock();
			cond = lock.newCondition();
			stop = false;
			
			// Loop for receive the words to translate 
			for(int i = 0; i < 10 ; i++) {
				retval = receive();
				if(retval == null) {
					commBox.setText("");
					btnSend.setEnabled(false);
					JOptionPane.showMessageDialog(contentPane, MyUtilities.MATCH_ERR);
					cl.show(contentPane.getContentPane(), "name_95218890939100");
					return;
				}
				
				if(retval.containsKey(MyUtilities.SERVER_ERROR_CODE)) {
					JOptionPane.showMessageDialog(contentPane, retval.get(MyUtilities.SERVER_ERROR_CODE));
					busy.set(false);
					commBox.setText("");
					btnSend.setEnabled(false);
					cl.show(contentPane.getContentPane(), "name_98987579688700");
					return;
				}
				else if(retval.containsKey(MyUtilities.UNAUTHORIZED_CODE)) {
					JOptionPane.showMessageDialog(contentPane, retval.get(MyUtilities.UNAUTHORIZED_CODE));
					busy.set(false);
					commBox.setText("");
					btnSend.setEnabled(false);
					cl.show(contentPane.getContentPane(), "name_98987579688700");
					return;
				}
				else if(retval.containsKey(MyUtilities.SUCCESS_CODE)) {
					JOptionPane.showMessageDialog(contentPane, retval.get(MyUtilities.SUCCESS_CODE));
					busy.set(false);
					commBox.setText("");
					btnSend.setEnabled(false);
					cl.show(contentPane.getContentPane(), "name_98987579688700");
					return;
				}
				else if(retval.containsKey(MyUtilities.TRANSLATE)) {
					StyledDocument document = (StyledDocument) commBox.getDocument();
				    try {
						document.insertString(document.getLength(), 
								MyUtilities.CHALLENGE_NUM + (i + 1) + MyUtilities.OF + 
								retval.get(MyUtilities.TRANSLATE), null);
					} catch (BadLocationException ble) {
						exit();
						commBox.setText("");
						btnSend.setEnabled(false);
						JOptionPane.showMessageDialog(contentPane, MyUtilities.SOMETHING_WRONG_CL);
						System.exit(-1);
					}
				    
				    // Enable the writing of translated word
				    lock.lock();
					canWrite = true;
					if(i == 9)
						stop = true;
					cond.signal();
					lock.unlock();
				}
			}
			
			retval = receive();
			if(retval == null) {
				commBox.setText("");
				btnSend.setEnabled(false);
				JOptionPane.showMessageDialog(contentPane, MyUtilities.MATCH_ERR);
				cl.show(contentPane.getContentPane(), "name_95218890939100");
				return;
			}
			
			if(retval.containsKey(MyUtilities.UNAUTHORIZED_CODE))
				JOptionPane.showMessageDialog(contentPane, retval.get(MyUtilities.UNAUTHORIZED_CODE));
			else if(retval.containsKey(MyUtilities.SUCCESS_CODE))
				JOptionPane.showMessageDialog(contentPane, retval.get(MyUtilities.SUCCESS_CODE));
		}
		
		busy.set(false);
		commBox.setText("");
		btnSend.setEnabled(false);
		cl.show(contentPane.getContentPane(), "name_98987579688700");
	}

	// Initialize the contents of the frame.
	private void initialize() {
		contentPane = new JFrame();
		contentPane.setBounds(100, 100, 450, 300);
		contentPane.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		contentPane.getContentPane().setLayout(new CardLayout(0, 0));
		
		JPanel rootJPanel = new JPanel();
		contentPane.getContentPane().add(rootJPanel, "name_95218890939100");
		rootJPanel.setLayout(null);

		JPanel matchPanel = new JPanel();
		contentPane.getContentPane().add(matchPanel, "name_267004884614900");
		matchPanel.setLayout(null);
		
		JSplitPane usrOp = new JSplitPane();
		contentPane.getContentPane().add(usrOp, "name_98987579688700");
		
		JSeparator separator = new JSeparator();
		separator.setOrientation(SwingConstants.VERTICAL);
		separator.setBackground(Color.BLACK);
		separator.setForeground(Color.BLACK);
		separator.setBounds(223, 0, 2, 261);
		rootJPanel.add(separator);
		
		JLabel lblNewLabel_1 = new JLabel("Michael De Angelis 560049 2019 - 2020");
		lblNewLabel_1.setFont(new Font("Tahoma", Font.PLAIN, 9));
		lblNewLabel_1.setBounds(0, 249, 188, 12);
		rootJPanel.add(lblNewLabel_1);
		
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

		JPanel panel = new JPanel();
		usrOp.setRightComponent(panel);
		panel.setLayout(null);		
		
		JLabel lblSe = new JLabel("Search user");
		lblSe.setBounds(100, 31, 113, 14);
		panel.add(lblSe);
		
		userSearch = new JTextField();
		userSearch.setBounds(100, 56, 214, 20);
		panel.add(userSearch);
		userSearch.setColumns(10);
				
		translatedWord = new JTextField();
		translatedWord.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnSend.doClick();
			}
		});
		translatedWord.setBounds(10, 228, 315, 20);
		matchPanel.add(translatedWord);
		translatedWord.setColumns(10);
		
		JTextPane commBox = new JTextPane();
		commBox.setBounds(10, 11, 414, 205);
		commBox.setEditable(false);
		matchPanel.add(commBox);

		// Send the sign up request and receive response 
		signUpButton = new JButton("Sign Up");
		signUpButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {		// Password confirmation
				if(!Arrays.equals(pswSignUp.getPassword(), confirmPswSignUp.getPassword())) {
					JOptionPane.showMessageDialog(contentPane, MyUtilities.USR_DIFFERENT_PSW);
					return;
				}
				
				try {
					Registry reg = LocateRegistry.getRegistry(MyUtilities.SIGN_UP_PORT);
					WqSignUp stub = (WqSignUp)reg.lookup(MyUtilities.SERVER_SIGN_UP_NAME);
					
					// Notify the response received by the server
					Map<String, String> retval = 
							jsonHandler.fromJson(stub.signUp(usrSignUp.getText(), String.valueOf(pswSignUp.getPassword())));
					
					if(retval.containsKey(MyUtilities.CLIENT_ERROR_CODE))
						JOptionPane.showMessageDialog(contentPane, MyUtilities.ERR_ENTERING_PARS + retval.get(MyUtilities.CLIENT_ERROR_CODE));
					else if(retval.containsKey(MyUtilities.RETRY_WITH_CODE))
						JOptionPane.showMessageDialog(contentPane, MyUtilities.URS_TAKEN + retval.get(MyUtilities.RETRY_WITH_CODE));
					else if(retval.containsKey(MyUtilities.SUCCESS_CODE)) {
						JOptionPane.showMessageDialog(contentPane, MyUtilities.SUCCESSFUL_REG + retval.get(MyUtilities.SUCCESS_CODE));
						
						// Clean the fields so the client can perform another registration without have to delete the previous information itself
						usrSignUp.setText(null);
						pswSignUp.setText(null);
						confirmPswSignUp.setText(null);
					}
				} catch (RemoteException | NotBoundException re_nbe) {
					JOptionPane.showMessageDialog(contentPane, MyUtilities.CONNECTION_FAIL);
				}
			}
		});
		signUpButton.setBounds(31, 207, 86, 31);
		rootJPanel.add(signUpButton);
		
		// Send the show score request and receive the server response
		btnShowScore = new JButton("Show Score");
		btnShowScore.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String showScoreRequest = jsonHandler.toJson(Collections.singletonMap(MyUtilities.SHOW_SCORE, userSearch.getText()));
				CardLayout cl = (CardLayout)contentPane.getContentPane().getLayout();
				
				Map<String, String> retval = sendAndReceive(showScoreRequest, MyUtilities.SHOW_SCORE_ERR_REQ);
				if(retval == null) {
					cl.show(contentPane.getContentPane(), "name_95218890939100");
					return;
				}
				
				if(retval.containsKey(MyUtilities.CLIENT_ERROR_CODE))
					JOptionPane.showMessageDialog(contentPane, MyUtilities.ERR_ENTERING_PARS + retval.get(MyUtilities.CLIENT_ERROR_CODE));
				else if(retval.containsKey(MyUtilities.SUCCESS_CODE))
					JOptionPane.showMessageDialog(contentPane, userSearch.getText() + MyUtilities.SPACE + retval.get(MyUtilities.SUCCESS_CODE));
			}
		});
		btnShowScore.setBounds(157, 121, 107, 23);
		panel.add(btnShowScore);
		
		// Handle the match request to the player and the show score request (only for friends players)
		JList<String> friendList = new JList<String>();
		friendList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				String opponent = friendList.getModel().getElementAt(friendList.getSelectedIndex());
				int retval = JOptionPane.showOptionDialog(contentPane, MyUtilities.WHAT_DU_WANT_TO_DO, MyUtilities.USER_SELECTED + 
						opponent, JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
				
				if(retval == 2 || retval == JOptionPane.CLOSED_OPTION)
					return;
				else if(retval == 1) {
					userSearch.setText(opponent);
					btnShowScore.doClick();
					userSearch.setText(null);
					return;
				}
				else {
					String matchRequest = jsonHandler.toJson(Collections.singletonMap(MyUtilities.MATCH, usr + MyUtilities.SPLIT + opponent));
					CardLayout cl = (CardLayout)contentPane.getContentPane().getLayout();
					
					Map<String, String> retvalRqst = sendAndReceive(matchRequest, MyUtilities.MATCH_ERR_SEND_REQ);
					if(retvalRqst == null) {
						cl.show(contentPane.getContentPane(), "name_95218890939100");
						return;
					}
					
					if(retvalRqst.containsKey(MyUtilities.CLIENT_ERROR_CODE))
						JOptionPane.showMessageDialog(contentPane, MyUtilities.ERR_ENTERING_PARS + retvalRqst.get(MyUtilities.CLIENT_ERROR_CODE));
					else if(retvalRqst.containsKey(MyUtilities.RETRY_WITH_CODE))
						JOptionPane.showMessageDialog(contentPane, retvalRqst.get(MyUtilities.RETRY_WITH_CODE));
					else if(retvalRqst.containsKey(MyUtilities.SERVER_ERROR_CODE))
						JOptionPane.showMessageDialog(contentPane, retvalRqst.get(MyUtilities.SERVER_ERROR_CODE));
					else if(retvalRqst.containsKey(MyUtilities.SUCCESS_CODE)) {
						busy.set(true);
						cl.show(contentPane.getContentPane(), "name_267004884614900");
						
						// Non modal advertisement displayed while waiting for the challenged's player response
						JOptionPane jop = new JOptionPane(MyUtilities.WAITING_FOR_MATCH_RP);
						JDialog dialog = jop.createDialog(contentPane, MyUtilities.MATCHMAKING);
						dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
						dialog.setModalityType(ModalityType.MODELESS);
						dialog.setVisible(true);
						
						new Thread() {
							@Override
							public void run() {
								Map<String, String> matchRetval = receive();
								if(matchRetval == null) {
									dialog.setVisible(false);
									JOptionPane.showMessageDialog(contentPane, MyUtilities.MATCH_ERR);
									cl.show(contentPane.getContentPane(), "name_95218890939100");
									return;
								}
								
								dialog.setVisible(false);

								if(matchRetval.containsKey(MyUtilities.RETRY_WITH_CODE)) {
									JOptionPane.showMessageDialog(contentPane, MyUtilities.MATCH_RQST_TIMEOUT);
									busy.set(false);
									cl.show(contentPane.getContentPane(), "name_98987579688700");
									return;
								}
								else if(matchRetval.containsKey(MyUtilities.SERVER_ERROR_CODE)) {
									JOptionPane.showMessageDialog(contentPane, matchRetval.get(MyUtilities.SERVER_ERROR_CODE));
									busy.set(false);
									cl.show(contentPane.getContentPane(), "name_98987579688700");
									return;
								}
								else if(matchRetval.containsKey(MyUtilities.MATCH)) {
									if(matchRetval.get(MyUtilities.MATCH).equals(MyUtilities.MATCH_REFUSED)) {
										JOptionPane.showMessageDialog(contentPane, MyUtilities.CLIENT_MATCH_REFUSED);
										busy.set(false);
										cl.show(contentPane.getContentPane(), "name_98987579688700");
										return;
									}
									
									handleMatch(commBox);
									return;
								}
								
								// This code should't be never reached
								busy.set(false);
							}
						}.start();
					}
				}
			}
		});
		friendList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		usrOp.setLeftComponent(friendList);

		// Send the friend list request and receive response; refresh the friend list displayed in the user interface
		btnRefresh = new JButton("Refresh");
		btnRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String friendListRequest = jsonHandler.toJson(Collections.singletonMap(MyUtilities.FRIEND_LIST, usr));
				CardLayout cl = (CardLayout)contentPane.getContentPane().getLayout();
				
				Map<String, String> retval = sendAndReceive(friendListRequest, MyUtilities.FRIEND_LIST_ERR_REQ);
				if(retval == null) {
					cl.show(contentPane.getContentPane(), "name_95218890939100");
					return;
				}
				
				if(retval.containsKey(MyUtilities.CLIENT_ERROR_CODE))
					JOptionPane.showMessageDialog(contentPane, MyUtilities.ERR_ENTERING_PARS + retval.get(MyUtilities.CLIENT_ERROR_CODE));
				else if(retval.containsKey(MyUtilities.SUCCESS_CODE)){		// Success
					ToClientLst toClientLst = new ToClientLst();
					DefaultListModel<String>  dml = new DefaultListModel<String>();
					
					for(ToClientLst.PlayerFriends pf : toClientLst.getClientLst(retval.get(MyUtilities.SUCCESS_CODE)))
						dml.addElement(pf.getUsr());
					
					friendList.setModel(dml);
					userSearch.setText(null);
				}
			}
		});
		btnRefresh.setBounds(109, 225, 89, 23);
		panel.add(btnRefresh);

		// Send the login request and receive the server response 
		btnLogin = new JButton("Login");
		btnLogin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {		// Open the connection with the Server and the 
					clientSocket = SocketChannel.open(new InetSocketAddress(InetAddress.getLoopbackAddress(), MyUtilities.TCP_CONTROL_PORT));
					clientUdpSocket = new DatagramSocket();
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(contentPane, MyUtilities.CONNECTION_FAIL);
					return;
				}
				
				// Create login request, send the request, receive the response
				String loginRequest = jsonHandler.toJson(Collections.singletonMap(MyUtilities.LOGIN, 
						usrLogin.getText() + MyUtilities.SPLIT + String.valueOf(pswLogin.getPassword())
						+ MyUtilities.SPLIT + String.valueOf(clientUdpSocket.getLocalPort())));
				
				Map<String, String> retval = sendAndReceive(loginRequest, MyUtilities.LOGIN_ERR_SEND_REQ);
				if(retval == null)
					return;
				
				usr = usrLogin.getText();
				
				if(retval.containsKey(MyUtilities.CLIENT_ERROR_CODE)) {
					JOptionPane.showMessageDialog(contentPane, MyUtilities.ERR_ENTERING_PARS + retval.get(MyUtilities.CLIENT_ERROR_CODE));
					exit();
				}
				else if(retval.containsKey(MyUtilities.UNAUTHORIZED_CODE)) {
					JOptionPane.showMessageDialog(contentPane, MyUtilities.CLIENT_ALREADY_ON);
					exit();
				}
				else if(retval.containsKey(MyUtilities.SUCCESS_CODE)) {
					JOptionPane.showMessageDialog(contentPane, MyUtilities.CLIENT_ONLINE + retval.get(MyUtilities.SUCCESS_CODE));
					
					usrLogin.setText(null);
					pswLogin.setText(null);
					
					CardLayout cl = (CardLayout)contentPane.getContentPane().getLayout();
					cl.show(contentPane.getContentPane(), "name_98987579688700");
					
					btnRefresh.doClick();
					
					// Run the thread that listen for udp messages (match request) and send the response
					new Thread() {
						@Override
						public void run() {
							while(true) {
								byte[] buf = new byte[MyUtilities.BYTE_ARRAY_SIZE];
								DatagramPacket matchReqPkt = new DatagramPacket(buf, buf.length);
								try {
									clientUdpSocket.receive(matchReqPkt);		// Wait for incoming match request
									
									Map<String, String> rqst = jsonHandler.fromJson(new String(matchReqPkt.getData()).trim());
									String rqstVal = rqst.get(MyUtilities.MATCH);
									if(rqstVal == null)							// Not a match request, the message is ignored
										continue;
									
									long timestamp = System.currentTimeMillis();
									
									// Show the match request only if the player is not busy on another match and if the user operation pane is displayed
									if(!busy.get() && usrOp.isShowing()) {
										int retval = JOptionPane.showConfirmDialog(contentPane, rqstVal + MyUtilities.DO_YOU_ACCEPT);
										if(retval == JOptionPane.CANCEL_OPTION || retval == JOptionPane.CLOSED_OPTION)
											continue;
										else if(retval == JOptionPane.NO_OPTION) {
											String refuse = jsonHandler.toJson(Collections.singletonMap(MyUtilities.MATCH, MyUtilities.MATCH_REFUSED));
											DatagramPacket refusePkt = new DatagramPacket(refuse.getBytes(), refuse.length(), InetAddress.getLoopbackAddress(), matchReqPkt.getPort());
											clientUdpSocket.send(refusePkt);
										}
										else {
											if(System.currentTimeMillis() < timestamp + MyUtilities.TIMEOUT) {
												String accept = jsonHandler.toJson(Collections.singletonMap(MyUtilities.MATCH, MyUtilities.MATCH_ACCEPTED));
												DatagramPacket acceptPkt = new DatagramPacket(accept.getBytes(), accept.length(), InetAddress.getLoopbackAddress(), matchReqPkt.getPort());
												clientUdpSocket.send(acceptPkt);
												
												cl.show(contentPane.getContentPane(), "name_267004884614900");
												new Thread() {
													@Override
													public void run() {
														handleMatch(commBox);
													}
												}.start();
											}
											else
												JOptionPane.showMessageDialog(contentPane, MyUtilities.MATCH_RQST_TIMEOUT);
										}
									}
								} catch (IOException ioe) {
									if(safeExit.get()) {
										safeExit.set(false);
										return;
									}
										
									JOptionPane.showMessageDialog(contentPane, MyUtilities.CLIENT_UDP_LIS_ERR);
									ErrorMacro.clientIoExceptionHandlingUDP(ioe, clientSocket, clientUdpSocket, usr, busy);
									System.exit(-1);
								}
							}
						}
					}.start();
				}
			}
		});
		btnLogin.setBounds(268, 181, 76, 31);
		rootJPanel.add(btnLogin);
		
		JLabel lblWordQuizzle = new JLabel("Word Quizzle");
		lblWordQuizzle.setFont(new Font("Yu Gothic UI", Font.BOLD | Font.ITALIC, 14));
		lblWordQuizzle.setBounds(340, 0, 94, 30);
		rootJPanel.add(lblWordQuizzle);
		
		// Send the logout request and receive the server response
		btnLogout = new JButton("Logout");
		btnLogout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String logoutRequest = jsonHandler.toJson(Collections.singletonMap(MyUtilities.LOGOUT, usr));
				CardLayout cl = (CardLayout)contentPane.getContentPane().getLayout();
				
				Map<String, String> retval = sendAndReceive(logoutRequest, MyUtilities.LOGOUT_ERR_SEND_REQ);
				if(retval == null) {
					cl.show(contentPane.getContentPane(), "name_95218890939100");
					return;
				}
				
				// With the GUI the fisrt two should't be displayed (no error(s))
				if(retval.containsKey(MyUtilities.CLIENT_ERROR_CODE)) 
					JOptionPane.showMessageDialog(contentPane, MyUtilities.ERR_ENTERING_PARS + retval.get(MyUtilities.CLIENT_ERROR_CODE));
				else if(retval.containsKey(MyUtilities.UNAUTHORIZED_CODE))
					JOptionPane.showMessageDialog(contentPane, MyUtilities.CLIENT_ALREADY_OFF);
				else if(retval.containsKey(MyUtilities.SUCCESS_CODE)) {
					JOptionPane.showMessageDialog(contentPane, MyUtilities.CLIENT_OFFLINE + retval.get(MyUtilities.SUCCESS_CODE));
					exit();
					cl.show(contentPane.getContentPane(), "name_95218890939100");
				}
			}
		});
		btnLogout.setBounds(208, 225, 89, 23);
		panel.add(btnLogout);
		
		// Send the add friend request and receive the server response
		btnAddFriend = new JButton("Add Friend");
		btnAddFriend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String addFriendRequest = jsonHandler.toJson(Collections.singletonMap(MyUtilities.ADD_FRIEND, usr + MyUtilities.SPLIT + userSearch.getText()));
				CardLayout cl = (CardLayout)contentPane.getContentPane().getLayout();
				
				Map<String, String> retval = sendAndReceive(addFriendRequest, MyUtilities.ADD_FRIEND_ERR_REQ);
				if(retval == null) {
					cl.show(contentPane.getContentPane(), "name_95218890939100");
					return;
				}
				
				if(retval.containsKey(MyUtilities.CLIENT_ERROR_CODE))
					JOptionPane.showMessageDialog(contentPane, MyUtilities.ERR_ENTERING_PARS + retval.get(MyUtilities.CLIENT_ERROR_CODE));
				else if(retval.containsKey(MyUtilities.SUCCESS_CODE)) {
					JOptionPane.showMessageDialog(contentPane, userSearch.getText() + MyUtilities.SPACE + retval.get(MyUtilities.SUCCESS_CODE));
					userSearch.setText(null);
				}
			}
		});
		btnAddFriend.setBounds(157, 87, 107, 23);
		panel.add(btnAddFriend);
		
		// Send the ranking request and receive the server response
		btnRanking = new JButton("Ranking");
		btnRanking.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String rankingRequest = jsonHandler.toJson(Collections.singletonMap(MyUtilities.SHOW_RANKING, usr));
				CardLayout cl = (CardLayout)contentPane.getContentPane().getLayout();
				
				Map<String, String> retval = sendAndReceive(rankingRequest, MyUtilities.RANKING_ERR_REQ);
				if(retval == null) {
					cl.show(contentPane.getContentPane(), "name_95218890939100");
					return;
				}
				
				if(retval.containsKey(MyUtilities.CLIENT_ERROR_CODE))
					JOptionPane.showMessageDialog(contentPane, MyUtilities.ERR_ENTERING_PARS + retval.get(MyUtilities.CLIENT_ERROR_CODE));
				else if(retval.containsKey(MyUtilities.SUCCESS_CODE)){
					ToClientLst toClientLst = new ToClientLst();
					StringBuilder strB = new StringBuilder();
					
					for(ToClientLst.PlayerFriends pf : toClientLst.getClientLst(retval.get(MyUtilities.SUCCESS_CODE)))
						strB.append(String.format("%s : %d" + System.lineSeparator(), pf.getUsr(), pf.getScore()));
					
					JOptionPane.showMessageDialog(contentPane, strB.toString());
				}
			}
		});
		btnRanking.setBounds(10, 225, 89, 23);
		panel.add(btnRanking);

		// Button activated only when the first word to translate is available
		btnSend = new JButton("Send");
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				CardLayout cl = (CardLayout)contentPane.getContentPane().getLayout();
				String translation = jsonHandler.toJson(Collections.singletonMap(MyUtilities.TRANSLATION, translatedWord.getText()));
				
				lock.lock();
				while(!canWrite) {
					try {
						if(!stop)
							cond.await();
						else 
							return;
					} catch (InterruptedException e) {
						return;
					}
				}
				
				// Exit safely in case of error
				if(!send(translation)) {
					commBox.setText("");
					btnSend.setEnabled(false);
					translatedWord.setText(null);
					JOptionPane.showMessageDialog(contentPane, MyUtilities.MATCH_ERR);
					cl.show(contentPane.getContentPane(), "name_95218890939100");
					return;
				}
				StyledDocument document = (StyledDocument) commBox.getDocument();
				try {
					document.insertString(document.getLength(), MyUtilities.GET_TEXT_RES + 
							translatedWord.getText() + System.lineSeparator(), null);
				} catch (BadLocationException ble) {
					exit();
					commBox.setText("");
					btnSend.setEnabled(false);
					translatedWord.setText(null);
					JOptionPane.showMessageDialog(contentPane, MyUtilities.SOMETHING_WRONG_CL);
					System.exit(-1);
				}
				canWrite = false;
				
				lock.unlock();
				translatedWord.setText(null);
			}
		});
		btnSend.setBounds(335, 227, 89, 23);
		btnSend.setEnabled(false);
		matchPanel.add(btnSend);
	}
}
