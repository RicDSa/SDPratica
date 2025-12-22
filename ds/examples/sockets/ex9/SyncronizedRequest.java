package ds.examples.sockets.ex9;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;
import java.util.logging.Logger;

public class SyncronizedRequest implements Runnable {

    private PoissonProcess poissonProcess = null;

    //Peer de Origem
    private final String host;
    private final int localport;
    private final Logger logger;

    //Peer de Destino
    private final int destinationPort;


    public SyncronizedRequest(String host, int localport, int destinationPort, Logger logger){
        this.host = host;
        this.localport = localport;
        this.destinationPort = destinationPort;
        this.logger = logger;

        //Inicializa o Processo de Poisson
        Random rng = new Random();
        double lambda = 1.0; //1 evento por minuto
        poissonProcess = new PoissonProcess(lambda, rng);
    }

    @Override
    public void run(){
        logger.info("Iniciou a Sincronizacao na porta @"+ localport);

        while(true){
            double intervalTime = poissonProcess.timeForNextEvent() * 1000 * 60; // Converter para milisegundos 

            try{
                Thread.sleep((long)intervalTime);

                //Envia um request de sincronizacao para o Peer na porta de destino
                sendRequestToServer("SYNC-DATA", destinationPort,localport);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void sendRequestToServer(String request, int serverport,int localport){
        String serverAddress = "localhost";

        try{

            Socket socket = new Socket(InetAddress.getByName(serverAddress), serverport);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            //Envia o request de sincronizacao
            out.println(request + ":" + localport);
            out.flush();


            socket.close();

        }catch (Exception e){
            logger.info("Server: Erro ao enviar o request para o localhost " + destinationPort);
        }

    }

}