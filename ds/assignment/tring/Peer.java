package ds.assignment.tring;

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
import java.util.LinkedList;
import java.util.Queue;



public class Peer {
    String host;
	String port;
    Logger logger;
	String token = "";

    public Peer(String host, String port) {
		this.host = host;
		this.port = port;
		

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
     * 
     * @param args  args Command-line arguments:
     * args[0] -> endeereço do peer atual
     * args[1] -> porta do peer atual
     * args[2] -> endereço do peer que queremos conectar
     * args[3] -> porta do peer que queremos conectar
     * args[4] -> endereço do servidor da calculadora
     * 
     * Examplo:
     * java Peer localhost 5000 localhost 6000
     * 
     * @throws Exception se ocorrer um erro durante a execução
     */
    public static void main(String[] args) throws Exception {
		
		Peer peer = new Peer(args[0], args[1]);
		System.out.printf("new peer @ host=%s port=%s\n", args[0], args[1]);


		Server server = new Server(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), args[4], peer.logger);
		new Thread(server).start();

		new Thread(new RequestGenerator(args[0], Integer.parseInt(args[1]), peer.logger, server)).start();
		
    }

}

class Server implements Runnable {
	//Atributos do Peer atual
    String       host;
    int          port;
    ServerSocket 	server;
	Peer 	peer;
    Logger  	logger;
	String receivedToken;
	Queue<String> operacoes = new LinkedList<>();

	// Endereço e porta do próximo Peer
	String VizinhoAddress;
	int VizinhoPort;

	// Endereço do Calculator
	String CalculatorAddress;
    
    public Server(String host, int port, String VizinhoAddress, int VizinhoPort, String CalculatorAddress, Logger logger) throws Exception {
		this.host   = host;
		this.port   = port;
		this.logger = logger;
		this.VizinhoAddress = VizinhoAddress;
		this.VizinhoPort = VizinhoPort;
		this.CalculatorAddress = CalculatorAddress;
        server = new ServerSocket(port, 1, InetAddress.getByName(host));
    }

	public void addOperacao(String operacao) {
		operacoes.add(operacao);
	}

    @Override
    public void run() {
	try {
	    logger.info("server: endpoint running at port " + port + " ...");
		final long TIMEOUT_MILLIS = 2 * 60 * 1000; //Tempo limite de 2 minutos
		long startTime = System.currentTimeMillis();

	    while(true) {
			logger.info("Cliente @" + host + " Porta @" + port + " a espera do Token...");

			try {
				server.setSoTimeout((int) (TIMEOUT_MILLIS - (System.currentTimeMillis() - startTime)));
				Socket client = server.accept();
				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				String receivedToken = in.readLine(); 
				
				//Verifica se o token foi recebido do peer anterior
				if(receivedToken.equals("Token")){
					logger.info("Received Token");
				}

				client.close();
			}catch(Exception e) {
				logger.warning("Timeout: O Token nao foi recebido no tempo limite.\nPeer will Shutdown ...");
                Thread.sleep(5000);
				e.printStackTrace();
			}    

			//Se recebeu o Token irá enviar as operações para o Calculator Server
			if(receivedToken.equals("Token")){
				startTime = System.currentTimeMillis(); //Faz reset ao timer porque recebeu o token

				//Envia as operações que tem para o Calculator Server
				synchronized(operacoes){
					//Será enviado todos os comandos na fila do Peer
					while(!operacoes.isEmpty()){
						String pedido = operacoes.poll();// Será retirado a operação que está em cima da lista e remove da lista
						String result = connectToCalculatorServer(CalculatorAddress, 33333, pedido);
						logger.info("Cliente @" + host + "Recebeu o resultado da calculadora: " + result);
					}

					Thread.sleep(4000);
				}
			}

			//Envia o Token para o Próximo Peer
			try {
				Socket socket = new Socket(VizinhoAddress, VizinhoPort);
				PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				out.println("Token"); //Envia o Token para o Vizinho
				out.flush();
				socket.close();
				receivedToken = null; // Faze reset à variável Token ou seja remove o token do peer atual
				logger.info("Cliente @" + host + " Enviou o Token para o Peer na porta" + VizinhoPort);

			}catch(Exception e) {
				System.out.printf("Coneccao entre o %s e o %s falhou!\n", host , VizinhoAddress);
			}

	    }
	} catch (Exception e) {
	     e.printStackTrace();
		}
    }

	//Função para fazer a conecção para o Calculator Server
	public String connectToCalculatorServer(String serverHost, int serverPort, String command) {
        String result = "";
        try {
            Socket socket = new Socket(InetAddress.getByName(serverHost), serverPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println(command + ":" + port);//Envia a operação para o calculator
            out.flush();
            result = in.readLine();//Recebe o resultado da operação
            socket.close();
        } catch (Exception e) {
            logger.info("client @" + port + " failed to connect to calculator server.");
            e.printStackTrace();
        }
        return result;
    }
}

//Serve para adicionar operações à fila do peer enquanto não tem o Token
class RequestGenerator implements Runnable {
	private final String host;
	private final int port;
	private final Logger logger;
	private final Server server;
	private PoissonProcess poissonprocess = null;


	public RequestGenerator (String host, int port, Logger logger, Server server){
		this.host = host;
		this.port = port;
		this.logger = logger;
		this.server = server;

		// Inicializa o processo de Poisson 
		Random rng = new Random();
		double lambda = 4.0;
		poissonprocess = new PoissonProcess(lambda, rng);

	}

	@Override
	public void run(){
		while(true){
			double intervalTime = poissonprocess.timeForNextEvent() * 1000 * 60;

			try {
				Thread.sleep((long)intervalTime);
				String request = generateRandomRequest(); // Gera operação

				//Adiciona a operação à fila de operações
				synchronized(request){
					server.addOperacao(request);
				}
			}catch (Exception e){
				e.printStackTrace();
			}
		}
	}

	//Função para gerar operações aleatórias
	private String generateRandomRequest() {

        String[] operations = {"add" , "sub" , "mul" , "div"};

        Random random = new Random();
        String operation = operations[random.nextInt(operations.length)];

        double x = Math.floor(random.nextDouble() * 100);;
        double y = Math.floor(random.nextDouble() * 100);;

        return operation + ":" + x + ":" + y;
    }
}