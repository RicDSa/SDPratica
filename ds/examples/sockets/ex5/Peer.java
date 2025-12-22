package ds.examples.sockets.ex5;

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
    Logger logger;

    public Peer(String hostname) {
		host   = hostname;
		logger = Logger.getLogger("logfile");
		try {
			FileHandler handler = new FileHandler("./" + hostname + "_peer.log", true);
			logger.addHandler(handler);
			SimpleFormatter formatter = new SimpleFormatter();	
			handler.setFormatter(formatter);	
		} catch ( Exception e ) {
			e.printStackTrace();
		}
    }
    
    public static void main(String[] args) throws Exception {
		Peer peer = new Peer(args[0]);
		System.out.printf("new peer @ host=%s\n", args[0]);

		int numberPeers = args.length-1;
		for(int i = 0; i <numberPeers; i++){
			new Thread(new Server(args[0], Integer.parseInt(args[1]), peer.logger)).start();
		}

		String[] availablePorts = Arrays.copyOfRange(args,1,args.length);
		for(int i = 0; i < numberPeers; i++){
			new Thread(new Client(args[0], availablePorts, peer.logger)).start();
		}
    }

}

class Server implements Runnable {
    String       host;
    int          port;
    ServerSocket server;
    Logger       logger;
    
    public Server(String host, int port, Logger logger) throws Exception {
		this.host   = host;
		this.port   = port;
		this.logger = logger;
        server = new ServerSocket(port, 1, InetAddress.getByName(host));
    }

    @Override
    public void run() {
	try {
	    logger.info("server: endpoint running at port " + port + " ...");
	    while(true) {
		try {
		    Socket client = server.accept();
		    String clientAddress = client.getInetAddress().getHostAddress();
		    logger.info("server: new connection from " + clientAddress);

		    new Thread(new Connection(clientAddress, client, logger)).start();
		}catch(Exception e) {
		    e.printStackTrace();
		}    
	    }
	} catch (Exception e) {
	     e.printStackTrace();
	}
    }
}

class Connection implements Runnable {
    String clientAddress;
    Socket clientSocket;
    Logger logger;

    public Connection(String clientAddress, Socket clientSocket, Logger logger) {
		this.clientAddress = clientAddress;
		this.clientSocket  = clientSocket;
		this.logger        = logger;
    }

    @Override
    public void run() {
	/*
	 * prepare socket I/O channels
	 */
	try {
	    	BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			
			String [] command = in.readLine().split(" ");

			logger.info("server: message from host " + clientAddress + "[command = " + displayCommand(command) + "]");
			/*
			 * parse command
			 */

			String op = command[2];
			double x = Double.valueOf(command[3]);
			double y = Double.valueOf(command[4]);
			double result = 0.0;
			/*
			 * execute op
			 */
			switch (op) {
				case "add":
					result = x + y;
					break;
				case "sub":
					result = x - y;
					break;
				case "mul":
					result = x * y;
					break;
				case "div":
					result = x / y;
					break;
			}
			/*
			 * send result
			 */
			
			out.println(String.valueOf(result));
			out.flush();
			//logger.info(String.valueOf(result));

			/*
			 * close connection
			 */
			clientSocket.close();
	} catch(Exception e) {
			e.printStackTrace();
		}
    }

	private String displayCommand(String[] command){
		String ans = "";
		for (String s : command) 
			ans += s + " ";
		return ans;
	}
}


class Client implements Runnable {
    
	private final String host;
    private String[] destinationPorts;
    private final Logger logger;
    private  PoissonProcess poissonProcess = null;
    
    public Client (String host, String[] destinationPorts, Logger logger){
		this.host    = host;
		this.destinationPorts = destinationPorts;
		this.logger  = logger; 
		
		//Intialize PoissonProcess
        Random rng = new Random();
        double lambda = 5.0;    
        poissonProcess = new PoissonProcess(lambda, rng);
    }

    @Override
    public void run() {
        while (true) {
            double intervalTime = poissonProcess.timeForNextEvent() * 1000 * 60; // Converting to milliseconds

            try {
                Thread.sleep((long)intervalTime);
				int randomPort = chooseRandomPort();
                String request = generateRandomRequest(randomPort);
                sendRequestToServer(request,randomPort);

            }catch (Exception e){
                e.printStackTrace(); 
            }

        }
    }

	private int chooseRandomPort(){
		Random random = new Random();
		int port = Integer.parseInt(destinationPorts[random.nextInt(destinationPorts.length)]);
		return port;
	}

	private void sendRequestToServer(String request, int serverport) {
        String serverAddress = "localhost";

        try{
            /*
             * make connection
             */
            Socket socket = new Socket(InetAddress.getByName(serverAddress), serverport);

			/*
			 * prepare socket I/O channels
			 */
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
            
            /*
             * send command
             */

            out.println(request);
            out.flush();
        
            /*
			* receive result
			*/
			String result = in.readLine();
			System.out.printf("Result = %f\n", Double.parseDouble(result));


            /*
             * close connection
             */

            socket.close();

        } catch (Exception e){
            //e.printStackTrace();
            logger.info("Server: error ocured while sending request");
        }
    }

	private String generateRandomRequest(int destinationPort) {

        String[] operations = {"add" , "sub" , "mul" , "div"};

        Random random = new Random();
        String operation = operations[random.nextInt(operations.length)];

        double x = random.nextDouble() * 100;
        double y = random.nextDouble() * 100;

        return host + " " + destinationPort + " " +  operation + " " + x + " " + y;
    }
}
