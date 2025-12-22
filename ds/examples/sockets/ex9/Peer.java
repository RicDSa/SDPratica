package ds.examples.sockets.ex9;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

public class Peer {
    String host;
	int port;
    Logger logger;

    public Peer(String hostname, String port) {
		host = hostname;
		this.port = Integer.parseInt(port);
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
		Peer peer = new Peer(args[0], args[1]);
		System.out.printf("new peer @ host=%s port=%s\n", args[0], args[1]);


		Server server = new Server(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), peer.logger);
		new Thread(server).start();

		SyncronizedRequest syncronizedRequest = new SyncronizedRequest(peer.host, peer.port, Integer.parseInt(args[2]), peer.logger);
		new Thread(syncronizedRequest).start();
		
		NumberGenerator numberGenerator = new NumberGenerator(peer.host, peer.port, peer.logger, server);
		new Thread(numberGenerator).start();
    }

}

class Server implements Runnable {
    String       host;
    int          port;
    ServerSocket 	server;
    Logger  	logger;
	Set<Integer> data = new HashSet<>();	// Conjunto de dados locais que queremos sincronizar
	Set<Integer> dataBeforeMerge = new HashSet<>();

	//Atributo dos peers que queremos sincronizar
	String nexthost = "localhost";
	int nextport;

    
    public Server(String host, int port, int nextport, Logger logger) throws Exception {
		this.host   = host;
		this.port   = port;
		this.nextport = nextport;
		this.logger = logger;
        server = new ServerSocket(port, 1, InetAddress.getByName(host));
    }

	public void addNumberToData(int n){
		data.add(n);
	}

    @Override
    public void run() {
	try {
	    logger.info("server: endpoint running at port " + port + " ...");
	    while(true) {
			try {
				Socket client = server.accept();
				String clientAddress = client.getInetAddress().getHostAddress();

				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				String request = in.readLine();

				Boolean updateSenderPeer = false;

				synchronized (data){

					if(isSyncronizationRequest(request)){
						int senderPort = getOriginPort(request);

						logger.info("Server: recebeu o request de sincronizacao de " + clientAddress + " @"+ senderPort);

						//Envia o conjunto de dados local para o peer vizinho (antes do merge)
						sendData(data);
					}
					else { // Recria o conjunto como string

						if(!request.equals("[]")){
							Set<Integer> resultSet = parseSetFromString(request);
							if(!resultSet.equals(data)){
								//guarda o conjunto de dados antes do merge
								dataBeforeMerge = new HashSet<>(data);

								data = mergeSet(resultSet, data);
								logger.info("Server: @" + port + " Conjunto local depois do MERGE: " + data.toString());
								updateSenderPeer = true;
							} 
						}
					}
				}

				// para alcançar o mesmo conjunto depois da sincronização entre peer's
				if(updateSenderPeer){
					synchronized(data){
						//Eniva os dados antes do Merge para poupar bandwith 
						sendData(dataBeforeMerge);
					}

				}
				
				}catch(Exception e) {
					e.printStackTrace();
				}    
	    	}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }


	private void sendData(Set aSet){
		try{
			Socket socket = new Socket(nexthost,nextport);

			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

			out.print(aSet);
			out.flush();

			socket.close();
		} catch (Exception e){
			logger.info("Server: Ocorreu um erro ao enviar a informacao ao Peer "+ nexthost + " @" + nextport);
			e.printStackTrace();
		}

	}

	private Set<Integer> parseSetFromString(String input){
		String content = input.substring(1, input.length() - 1).trim();

		return Arrays.stream(content.split("\\s*,\\s*")).map(Integer::parseInt).collect(Collectors.toSet());
	}

	public <T> Set<T> mergeSet(Set<T> a, Set<T> b){
		
		return new HashSet<T>(){
			{
				addAll(a);
				addAll(b);
			}
		};

	}

	private Boolean isSyncronizationRequest(String request) {
		return request.contains("SYNC-DATA");
	}

	private int getOriginPort(String request){
		Scanner sc = new Scanner(request).useDelimiter(":");
		sc.next();
		String ans = sc.next();

		return Integer.parseInt(ans);
	}
}

