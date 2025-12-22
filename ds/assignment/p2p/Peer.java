package ds.assignment.p2p;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeSet;
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

    /**
	 * args[0]   -> Endereço Peer atual
	 * args[1]   -> Porta do Peer local
	 * args[i] 	 -> Endereço do Peer vizinho
	 * args[i+1] -> Porta do Peer vizinho
	 * e.g java Peer localhost 22222 localhost 33333 localhost 44444 localhost 55555
	 * @param args
	 * @throws Exception
	 */
    public static void main(String[] args) throws Exception {
		Peer peer = new Peer(args[0], args[1]);
		System.out.printf("new peer @ host=%s port=%s\n", args[0], args[1]);

		// Create Peer Connection
		PeerConnection vizinhosInfo = new PeerConnection();
		for(int i = 2; i < args.length; i+=2){
			peer.logger.info("novo vizinho "+ args[i] + " @" +args[i+1]);
			vizinhosInfo.addVizinho(Integer.parseInt(args[i+1]), args[i]);
		}

		double valorInicial = Double.parseDouble(args[args.length - 1]);

		Server server = new Server(args[0], Integer.parseInt(args[1]), vizinhosInfo, peer.logger, valorInicial);
		new Thread(server).start();

		SyncronizedRequest syncronizedRequest = new SyncronizedRequest(peer.host, peer.port, vizinhosInfo, peer.logger, server);
		new Thread(syncronizedRequest).start();
		
    }

}

class Server implements Runnable {
    String       host;
    int          port;
    ServerSocket 	server;
    Logger  	logger;
	double myValue;

	Map<String, Long> data;

	//Atributo dos peers que queremos sincronizar
	PeerConnection vizinhosInfo;
	String nexthost;
	int nextport;

    
    public Server(String host, int port, PeerConnection vizinhosInfo, Logger logger, double initialValue) throws Exception {
		this.host   = host;
		this.port   = port;
		this.vizinhosInfo = vizinhosInfo;
		this.logger = logger;
		this.myValue = initialValue;
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

				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				String request = in.readLine();

				synchronized (data){

					if(isSyncronizationRequest(request)){
						int senderPort = getOriginPort(request);

						logger.info("Server: recebeu o request de sincronizacao de " + clientAddress + " @"+ senderPort);

						// Updates the Info of peer that we are going to send data set
						// for syncronization
						nexthost = getOriginHost(request);
						nextport = getOriginPort(request);

						//Envia o conjunto de dados local para o peer vizinho (antes do merge)
						sendData(data);
					}
					// If this peer is the one receiving the syncronization response 
					// that includes the sender's Set 
					else if(isMessageForThisPeer(request)){ 

						if(!request.contains("[]")){
							Set<Integer> resultSet = parseSetFromString(request);
							if(!resultSet.equals(data)){
								//guarda o conjunto de dados antes do merge
								dataBeforeMerge = new HashSet<>(data);

								// Updates the Info of peer that we are going to send data set
								// for syncronization
								nexthost = getOriginHost(request);
								nextport = getOriginPort(request);

								data = mergeSet(resultSet, data);

								// Sort data set (Ajuda na visualização)
								List<Integer> sortedData = new ArrayList<>(data);
								Collections.sort(sortedData);

								logger.info("Server: @" + port + " Conjunto local depois do MERGE: " + sortedData);

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

	public synchronized double getValue() {
    	return myValue;
	}

	public synchronized void updateValue(double receivedValue) {
		// A fórmula de agregação: média entre o local e o vizinho
		this.myValue = (this.myValue + receivedValue) / 2.0;
		System.out.println("Novo valor agregado: " + this.myValue);
	}
}

