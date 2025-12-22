package ds.examples.sockets.ex9b;

import java.util.Random;
import java.util.logging.Logger;

public class  NumberGenerator implements Runnable {

    private final String host;
    private final int localport;
    private final Logger logger;
    private final Server server;
    private PoissonProcess poissonProcess = null;

    public NumberGenerator(String host, int localport, Logger logger, Server server){
        this.host = host;
        this.localport = localport;
        this.logger = logger;
        this.server = server;

        //Inicializa o Processo de Poisson
        Random rng = new Random();
        double lambda = 4.0;
        poissonProcess = new PoissonProcess(lambda, rng);
    }

    @Override
    public void run(){
        while(true){
            double intervalTime = poissonProcess.timeForNextEvent() * 1000 * 60;

            try{
                Thread.sleep((long)intervalTime);
                Integer number = generateRandomNumber();


                synchronized(number){
                    server.addNumberToData(number);
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private int generateRandomNumber(){
        return (int)(Math.random() * 10000);
    }

}