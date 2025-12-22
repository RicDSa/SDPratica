package ds.examples.sockets.ex8;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.Arrays;
import java.util.Random;
import java.lang.Thread;

public class Peer {
    String host;
	String port;
    Logger logger;
	String token = "";

    public Peer(String host, String port, boolean hasToken) {
		this.host = host;
		this.port = port;
		token = hasToken ? "Token" : ""; 

		logger = Logger.getLogger("logfile");
		try {
			FileHandler handler = new FileHandler("./" + host + "_peer.log", true);
			logger.addHandler(handler);
			SimpleFormatter formatter = new SimpleFormatter();	
			handler.setFormatter(formatter);	
		} catch ( Exception e ) {
			e.printStackTrace();
		}
    }
    
    public static void main(String[] args) throws Exception {
		boolean hasToken = args[2].equals("Token");
		Peer peer = new Peer(args[0], args[1], hasToken);
		System.out.printf("new peer @ host=%s port=%s token=%s\n", args[0], args[1], hasToken);


		new Thread(new Server(args[0], Integer.parseInt(args[1]), peer)).start();
		new Thread(new Client(peer, args[3], Integer.parseInt(args[4]))).start();
    }

}

class Server implements Runnable {
    String       host;
    int          port;
    ServerSocket 	server;
	Peer 	peer;
    Logger  	logger;
    
    public Server(String host, int port, Peer peer) throws Exception {
		this.host   = host;
		this.port   = port;
		this.peer = peer;
		this.logger = peer.logger;
        server = new ServerSocket(port, 1, InetAddress.getByName(host));
    }

    @Override
    public void run() {
	try {
	    logger.info("server: endpoint running at port " + port + " ...");
	    while(true) {
			try {
				Socket client = server.accept();
				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				String receivedToken = in.readLine(); //Recebe o token
				
				if(receivedToken.equals("Token")){
					logger.info("Received Token");
					peer.token = "Token";
				}

				client.close();
			}catch(Exception e) {
				e.printStackTrace();
			}    
	    }
	} catch (Exception e) {
	     e.printStackTrace();
	}
    }
}

class Client implements Runnable {
    
	String host;
    String port;
	Peer peer;
	String otherpeerhost;
	int otherpeerport;
    Logger logger;
    boolean peernotconnected = false;
    
    public Client (Peer peer, String otherpeerhost, int otherpeerport){
		this.peer    = peer;
		this.host = peer.host;
		this.port = peer.port;
		this.otherpeerhost = otherpeerhost;
		this.otherpeerport = otherpeerport;
		this.logger = peer.logger; 
	
    }

    @Override
    public void run() {
        while (true) {
            try {
				if("Token".equals(peer.token)){

					//Envia o pedido para a calculadora e recebe o resultado
					if(sendCommandToCalculator()){
						String result = connectToServer("localhost", 44444, generateRandomRequest());
						logger.info("Client: " + port + " Result from calculator:" + result);
					}

					Thread.sleep(5000);
				

				//Enviar token para o próximo peer
				try {
					Socket socket = new Socket(InetAddress.getByName(otherpeerhost), otherpeerport);
					PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
					out.println("Token");
					out.flush();
					socket.close(); 

					//Depois de enviar o token remove
					peer.token = "";
					logger.info("Client: " + host + "enviou o token para a porta " + otherpeerport);
				} catch (IOException e){
					if (!peernotconnected)
                            System.out.println("client: " + otherpeerhost + " @" + otherpeerport + " esta OFFLINE");
                        peernotconnected = true;
				}
			} else {
				logger.info("Client:"+ host +"Waiting for token...");
				Thread.sleep(3000);
			}

            }catch (Exception e){
                e.printStackTrace(); 
            }

        }
    }

	private String connectToServer(String serverhost, int serverport, String command) {
        String result = "";

        try{
            /*
             * make connection
             */
            Socket socket = new Socket(InetAddress.getByName(serverhost), serverport);

			/*
			 * prepare socket I/O channels
			 */
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
            
            /*
             * send command
             */

            out.println(command + ":" + port);
            out.flush();
        
            /*
			* receive result
			*/
			result = in.readLine();


            /*
             * close connection
             */

            socket.close();

        } catch (Exception e){
            //e.printStackTrace();
            logger.info("Server: error ocured while sending request");
        }

		return result;
    }

	private String generateRandomRequest() {

        String[] operations = {"add" , "sub" , "mul" , "div"};

        Random random = new Random();
        String operation = operations[random.nextInt(operations.length)];

        double x = Math.floor(random.nextDouble() * 100);;
        double y = Math.floor(random.nextDouble() * 100);;

        return operation + ":" + x + ":" + y;
    }

	static boolean sendCommandToCalculator(){
        return coinToss() == 1;
    }

	static int coinToss(){
        return (int)Math.round(Math.random());
    }
}