import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collections;

public class Client2 {

	public static void main(String[] args) throws IOException, NotBoundException {
		JsonHandler gson = new JsonHandler();
		
		Registry reg = LocateRegistry.getRegistry(8080);
		WqSignUp stub = (WqSignUp)reg.lookup("WQ_SIGN_UP");
		String printSignUp2 = stub.signUp("Maxim", "enumerazione");
		System.out.println(printSignUp2);
		
		SocketChannel clientSocket = SocketChannel.open(new InetSocketAddress(InetAddress.getLoopbackAddress(), 9999));
		DatagramSocket clientUdpSock = new DatagramSocket();
		
		String port = String.valueOf(clientUdpSock.getLocalPort());
		// LOGIN
		System.out.println(port);
		String loginRequest = gson.toJson(Collections.singletonMap("LOGIN", "Maxim,enumerazione," + port));
		ByteBuffer loginLength = ByteBuffer.allocate(Integer.BYTES);
		loginLength.putInt(loginRequest.length());
		loginLength.flip();
		clientSocket.write(loginLength);
		
		ByteBuffer requestLogin = ByteBuffer.wrap(loginRequest.getBytes());
		clientSocket.write(requestLogin);
		
		ByteBuffer responseLengthLogin = ByteBuffer.allocate(Integer.BYTES);
		clientSocket.read(responseLengthLogin);
		responseLengthLogin.flip();
		ByteBuffer resLogin = ByteBuffer.allocate(responseLengthLogin.getInt());
		clientSocket.read(resLogin);
		
		String printLogin = new String(resLogin.array()).trim();
		System.out.println(printLogin);

		byte[] bufResponse = new byte[512];
		DatagramPacket requestChall = new DatagramPacket(bufResponse, bufResponse.length);
		clientUdpSock.receive(requestChall);
		
		String req = new String(requestChall.getData()).trim();
		System.out.println(req);
		
		String accept = gson.toJson(Collections.singletonMap(MyUtilities.SUCCESS_CODE, MyUtilities.MATCH_ACCEPTED));
		DatagramPacket acceptDp = new DatagramPacket(accept.getBytes(), accept.length(), InetAddress.getLoopbackAddress(), requestChall.getPort());
		clientUdpSock.send(acceptDp);
		
		ByteBuffer responseLengthBegin = ByteBuffer.allocate(Integer.BYTES);
		clientSocket.read(responseLengthBegin);
		responseLengthBegin.flip();
		ByteBuffer resBegin = ByteBuffer.allocate(responseLengthBegin.getInt());
		clientSocket.read(resBegin);
		
		String printBegin = new String(resBegin.array()).trim();
		System.out.println(printBegin);
		
		for(int i = 0; i < 10; i++) {
			ByteBuffer responseLengthW = ByteBuffer.allocate(Integer.BYTES);
			clientSocket.read(responseLengthW);
			responseLengthW.flip();
			ByteBuffer resW = ByteBuffer.allocate(responseLengthW.getInt());
			clientSocket.read(resW);
			
			String printW = new String(resW.array()).trim();
			System.out.println(printW);
			
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String str = gson.toJson(Collections.singletonMap(MyUtilities.TRANSLATION, br.readLine()));
			
			ByteBuffer strLength = ByteBuffer.allocate(Integer.BYTES);
			strLength.putInt(str.length());
			strLength.flip();
			int nwrite = clientSocket.write(strLength);
			System.out.println("Scritti " + nwrite + " byte");
			
			ByteBuffer translation = ByteBuffer.wrap(str.getBytes());
			nwrite = clientSocket.write(translation);
			System.out.println("Scritti " + nwrite + " byte");
			
			System.out.println("HO SCRITTO " + str);
		}
		
		clientSocket.close();
		clientUdpSock.close();
	}

}
