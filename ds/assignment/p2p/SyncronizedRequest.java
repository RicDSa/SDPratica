package ds.assignment.p2p;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Logger;

public class SyncronizedRequest implements Runnable {

    private PoissonProcess poissonProcess = null;

    //Peer de Origem
    private final String host;
    private final int localport;
    private final Logger logger;

    //Peer de Destino
    private int destinationPort;
    private String destinationHost;
    private PeerConnection vizinhosInfo;

    Server server;


    public SyncronizedRequest(String host, int localport, PeerConnection vizinhosInfo, Logger logger, Server server){
        this.host = host;
        this.localport = localport;
        this.vizinhosInfo = vizinhosInfo;
        this.logger = logger;
        this.server = server;

        //Inicializa o Processo de Poisson
        Random rng = new Random();
        double lambda = 2.0; //2 eventos por minuto
        poissonProcess = new PoissonProcess(lambda, rng);
    }

    @Override
    public void run(){
        logger.info("Iniciou a Sincronizacao na porta @"+ localport);

        while(true){
            double intervalTime = poissonProcess.timeForNextEvent() * 1000 * 60; // Converter para milisegundos 

            try{
                Thread.sleep((long)intervalTime);

                //format: "hostname:port"
                // choose a random neighbour peer to do Syncronization
                String n = vizinhosInfo.chooseRandomVizinho();

                Scanner sc = new Scanner(n).useDelimiter(":");
                destinationHost = sc.next();
                destinationPort = Integer.parseInt(sc.next());

                //Envia um request de sincronizacao para o Peer na porta de destino
                sendRequestToServer("SYNC-DATA");
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void sendRequestToServer(String request){
       
        try{

            Socket socket = new Socket(destinationHost, destinationPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 1. Envia o meu valor atual
            double meuValor = server.getValue();
            out.println("SYNC:" + meuValor);

            // 2. Lê o valor do vizinho
            String response = in.readLine(); // Espera algo como "VAL:0.5"
            if (response != null && response.startsWith("VAL:")) {
                double vizinhoValor = Double.parseDouble(response.split(":")[1]);
        
                // 3. Atualiza o meu valor (Média)
                server.updateValue(vizinhoValor);
            }

        }catch (Exception e){
            logger.info("Server: Erro ao enviar o request para o "+ destinationHost + " " + destinationPort);
        }

    }

}