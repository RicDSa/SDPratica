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
		for(int i = 2; i < args.length - 1; i+=2){
			peer.logger.info("novo vizinho "+ args[i] + " @" +args[i+1]);
			vizinhosInfo.addVizinho(Integer.parseInt(args[i+1]), args[i]);
		}

		double valorInicial = Double.parseDouble(args[args.length - 1]);

		Server server = new Server(args[0], Integer.parseInt(args[1]), peer.logger, valorInicial);
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

	//Atributo dos peers que queremos sincronizar
	String nexthost;
	int nextport;

    
    public Server(String host, int port, Logger logger, double initialValue) throws Exception {
		this.host   = host;
		this.port   = port;
		this.logger = logger;
		this.myValue = initialValue;
        this.server = new ServerSocket(port, 1, InetAddress.getByName(host));
    }

    @Override
    public void run() {
	try {
	    logger.info("server: endpoint running at port " + port + " ...");
	    while(true) {
			try {
				Socket client = server.accept();
                    
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);

                String request = in.readLine();

                    if (request != null && request.startsWith("SYNC:")) {
                        double vizinhoValor = Double.parseDouble(request.split(":")[1]);

                        // 1. Responder com o meu valor ATUAL (antes da média)
                        //    Usamos synchronized block para garantir que lemos o valor correto
                        double valorParaEnviar;
                        synchronized(this) {
                            valorParaEnviar = this.myValue;
                        }
                        out.println("VAL:" + valorParaEnviar);

                        // 2. Atualizar o meu valor
                        updateValue(vizinhoValor);
                    }
                    
                    client.close();
                } catch (Exception e) {
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
		// Média entre o local e o vizinho
		this.myValue = (this.myValue + receivedValue) / 2.0;
		System.out.println("Novo valor: " + this.myValue);
	}
}

