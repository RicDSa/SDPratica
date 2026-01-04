package ds.assignment.tom;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class PeerConnection {


    PeerConnection(){

    }


    // Key-> Port
    //value-> hostname
    private Map <Integer, String> vizinhos = new HashMap<>();


    public Map<Integer, String> getVizinhos(){
        return vizinhos;
    }

    public void addVizinho(int port,String address){
        vizinhos.put(port, address);
    }


    /**
     * 
     * @return a string in the format: "hostname:port" of 
     * available neighbours
     */
    public String chooseRandomVizinho(){
        List <Integer> ports = new ArrayList<>(vizinhos.keySet());

        Random rand = new Random();

        int randomInt = rand.nextInt(ports.size());
        int randomPort = ports.get(randomInt);

        String address = vizinhos.get(randomPort);
        return address + ":" + randomPort;
    }

}