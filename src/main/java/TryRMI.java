import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collections;

public class TryRMI {

	public static void main(String[] args) throws NotBoundException, UnsupportedEncodingException, IOException {
		JsonHandler gson = new JsonHandler();
		
		Registry reg = LocateRegistry.getRegistry(8080);
		WqSignUp stub = (WqSignUp)reg.lookup("WQ_SIGN_UP");
		
		String printSignUp3 = stub.signUp("Michael", "alfabeto");
		System.out.println(printSignUp3);
		
		
		SocketChannel clientSocket = SocketChannel.open(new InetSocketAddress(InetAddress.getLoopbackAddress(), 9999));
		DatagramSocket clientUdpSock = new DatagramSocket();
		
		String port = String.valueOf(clientUdpSock.getLocalPort());
		
		// LOGIN
		
		String loginRequest = gson.toJson(Collections.singletonMap("LOGIN", "Michael,alfabeto," + port));
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
		
		
		// ADD FRIEND
		
		String addFriendRequest = gson.toJson(Collections.singletonMap("ADD_FRIEND", "Michael,Maxim"));
		ByteBuffer addFriendLength = ByteBuffer.allocate(Integer.BYTES);
		addFriendLength.putInt(addFriendRequest.length());
		addFriendLength.flip();
		clientSocket.write(addFriendLength);
		
		ByteBuffer requestAddFriend = ByteBuffer.wrap(addFriendRequest.getBytes());
		clientSocket.write(requestAddFriend);
		
		ByteBuffer responseLengthAddFriend = ByteBuffer.allocate(Integer.BYTES);
		clientSocket.read(responseLengthAddFriend);
		responseLengthAddFriend.flip();
		ByteBuffer resAddFriend = ByteBuffer.allocate(responseLengthAddFriend.getInt());
		clientSocket.read(resAddFriend);
		
		String printAddFriend = new String(resAddFriend.array()).trim();
		System.out.println(printAddFriend);
		
		/**
		// FRIEND LIST
		
		String friendLstRequest = gson.toJson(Collections.singletonMap("FRIEND_LIST", "Michael"));
		ByteBuffer friendLstLength = ByteBuffer.allocate(Integer.BYTES);
		friendLstLength.putInt(friendLstRequest.length());
		friendLstLength.flip();
		clientSocket.write(friendLstLength);
		
		ByteBuffer requestFriendLst = ByteBuffer.wrap(friendLstRequest.getBytes());
		clientSocket.write(requestFriendLst);
		
		ByteBuffer responseLengthFriendLst = ByteBuffer.allocate(Integer.BYTES);
		clientSocket.read(responseLengthFriendLst);
		responseLengthFriendLst.flip();
		ByteBuffer resFriendLst = ByteBuffer.allocate(responseLengthFriendLst.getInt());
		clientSocket.read(resFriendLst);
		
		String printFriendLst = new String(resFriendLst.array()).trim();
		System.out.println(printFriendLst);
		**/
		
		// MATCH
		
		String matchRequest = gson.toJson(Collections.singletonMap("MATCH", "Michael,Maxim"));
		ByteBuffer matchLength = ByteBuffer.allocate(Integer.BYTES);
		matchLength.putInt(matchRequest.length());
		matchLength.flip();
		clientSocket.write(matchLength);
		
		ByteBuffer requestMatch = ByteBuffer.wrap(matchRequest.getBytes());
		clientSocket.write(requestMatch);
		
		ByteBuffer responseLengthMatch = ByteBuffer.allocate(Integer.BYTES);
		clientSocket.read(responseLengthMatch);
		responseLengthMatch.flip();
		ByteBuffer resMatch = ByteBuffer.allocate(responseLengthMatch.getInt());
		clientSocket.read(resMatch);
		
		String printMatch = new String(resMatch.array()).trim();
		System.out.println(printMatch);
		
		if(printMatch.contains("SEND")) {
			ByteBuffer responseLengthMatch2 = ByteBuffer.allocate(Integer.BYTES);
			clientSocket.read(responseLengthMatch2);
			responseLengthMatch2.flip();
			ByteBuffer resMatch2 = ByteBuffer.allocate(responseLengthMatch2.getInt());
			clientSocket.read(resMatch2);
			
			String printMatch2 = new String(resMatch2.array()).trim();
			System.out.println(printMatch2);
			
			if(printMatch2.contains("ACCEPTED")) {
				ByteBuffer responseLengthMatch3 = ByteBuffer.allocate(Integer.BYTES);
				clientSocket.read(responseLengthMatch3);
				responseLengthMatch3.flip();
				ByteBuffer resMatch3 = ByteBuffer.allocate(responseLengthMatch3.getInt());
				clientSocket.read(resMatch3);
				
				String printMatch3 = new String(resMatch3.array()).trim();
				System.out.println(printMatch3);
				
				if(printMatch3.contains("BEGIN")) {
					for(int i = 0; i < 10; i++) {
						ByteBuffer responseLengthMatch4 = ByteBuffer.allocate(Integer.BYTES);
						clientSocket.read(responseLengthMatch4);
						responseLengthMatch4.flip();
						ByteBuffer resMatch4 = ByteBuffer.allocate(responseLengthMatch4.getInt());
						clientSocket.read(resMatch4);
						
						String printMatch4 = new String(resMatch4.array()).trim();
						System.out.println(printMatch4);
						
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
					
					ByteBuffer responseLengthMatch4 = ByteBuffer.allocate(Integer.BYTES);
					clientSocket.read(responseLengthMatch4);
					responseLengthMatch4.flip();
					ByteBuffer resMatch4 = ByteBuffer.allocate(responseLengthMatch4.getInt());
					clientSocket.read(resMatch4);
					
					String printMatch4 = new String(resMatch4.array()).trim();
					System.out.println(printMatch4);
				}
			}
		}
		
		
		// USER SCORE
		
		String scoreRequest = gson.toJson(Collections.singletonMap("SHOW_SCORE", "Maxim"));
		ByteBuffer scoreLength = ByteBuffer.allocate(Integer.BYTES);
		scoreLength.putInt(scoreRequest.length());
		scoreLength.flip();
		clientSocket.write(scoreLength);
		
		ByteBuffer requestScore = ByteBuffer.wrap(scoreRequest.getBytes());
		clientSocket.write(requestScore);
		
		ByteBuffer responseLengthScore = ByteBuffer.allocate(Integer.BYTES);
		clientSocket.read(responseLengthScore);
		responseLengthScore.flip();
		ByteBuffer resScore = ByteBuffer.allocate(responseLengthScore.getInt());
		clientSocket.read(resScore);
		
		String printScore = new String(resScore.array()).trim();
		System.out.println(printScore);
		
		/**
		// RANKING LIST
		
		String rankingRequest = gson.toJson(Collections.singletonMap("SHOW_RANKING", "Michael"));
		ByteBuffer rankingLength = ByteBuffer.allocate(Integer.BYTES);
		rankingLength.putInt(rankingRequest.length());
		rankingLength.flip();
		clientSocket.write(rankingLength);
		
		ByteBuffer requestRanking = ByteBuffer.wrap(rankingRequest.getBytes());
		clientSocket.write(requestRanking);
		
		ByteBuffer responseLengthRanking = ByteBuffer.allocate(Integer.BYTES);
		clientSocket.read(responseLengthRanking);
		responseLengthRanking.flip();
		ByteBuffer resRanking = ByteBuffer.allocate(responseLengthRanking.getInt());
		clientSocket.read(resRanking);
		
		String printRanking = new String(resRanking.array()).trim();
		System.out.println(printRanking);
		
		// LOGOUT
		
		String logoutRequest = gson.toJson(Collections.singletonMap("LOGOUT", "Michael"));
		ByteBuffer logoutLength = ByteBuffer.allocate(Integer.BYTES);
		logoutLength.putInt(logoutRequest.length());
		logoutLength.flip();
		clientSocket.write(logoutLength);
		
		ByteBuffer requestLogout = ByteBuffer.wrap(logoutRequest.getBytes());
		clientSocket.write(requestLogout);
		
		ByteBuffer responseLengthLogout = ByteBuffer.allocate(Integer.BYTES);
		clientSocket.read(responseLengthLogout);
		responseLengthLogout.flip();
		ByteBuffer resLogout = ByteBuffer.allocate(responseLengthLogout.getInt());
		clientSocket.read(resLogout);
		
		String printLogout = new String(resLogout.array()).trim();
		System.out.println(printLogout);
		**/
		clientSocket.close();
		clientUdpSock.close();
	}
}
