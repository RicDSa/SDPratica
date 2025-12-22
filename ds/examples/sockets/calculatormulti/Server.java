package ds.examples.sockets.calculatormulti;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.Arrays;

public class Server {
    private ServerSocket server;

    public Server(String ipAddress) throws Exception {
        this.server = new ServerSocket(0, 1, InetAddress.getByName(ipAddress));
    }

    private void listen() throws Exception {
		while(true) {
			Socket client = this.server.accept();
			String clientAddress = client.getInetAddress().getHostAddress();
			System.out.printf("\r\nnew connection from %s\n", clientAddress);
			new Thread(new ConnectionHandler(clientAddress, client)).start();
		}
    }
    
    public InetAddress getSocketAddress() {
	return this.server.getInetAddress();
    }
        
    public int getPort() {
	return this.server.getLocalPort();
    }
    
    public static void main(String[] args) throws Exception {
	Server app = new Server(args[0]);
	System.out.printf("\r\nrunning server: host=%s @ port=%d\n",
	    app.getSocketAddress().getHostAddress(), app.getPort());
	app.listen();
    }
}



class ConnectionHandler implements Runnable {
    String clientAddress;
    Socket clientSocket;    

    public ConnectionHandler(String clientAddress, Socket clientSocket) {
	this.clientAddress = clientAddress;
	this.clientSocket  = clientSocket;    
    }

    @Override
    public void run() {
	/*
	 * prepare socket I/O channels
	 */
	try {
	    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));    
	    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
	
	    while(true) {
		/* 
		 * receive command 
		 */
		String command;
		if( (command = in.readLine()) == null)
		    break;
		else
		    System.out.printf("message from %s : %s\n", clientAddress, command);		      	                    /*
																     * process command 
																     */
		Scanner sc = new Scanner(command).useDelimiter(":");
		String  op = sc.next();
		String  x  = sc.next();
		String  y  = sc.next();
		

		switch(op) {
			case "leng": 
				out.println(String.valueOf(x.length())); 
				out.flush(); 
				break;
			case "equal": 
				if(x.equals(y) == true){
					out.println("Sao iguais");
				}else {
					out.println("Sao diferentes");
				}
				out.flush(); 
				break;
			case "cat": 
				String result = x.concat(y); 
				out.println(result);
				out.flush(); 
				break;
			case "break": 
				result = Arrays.toString(x.split(y));
				out.println(result);
				out.flush();
				break;
		}  
		
	    }
		}catch(Exception e) {
			e.printStackTrace();
		}
    }
}
