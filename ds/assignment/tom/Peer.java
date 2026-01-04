package ds.assignment.tom;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap; 
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import ds.assignment.tom.PeerConnection;

public class Peer {
    String id;
    String host;
    int port;
    Logger logger;
    
    LamportClock clock;
    PeerConnection allPeers;
    PriorityBlockingQueue<Message> queue;
    
    // Mapa thread-safe oficial do Java
    Map<String, Integer> lastTimeStampFromPeer; 
    
    static final List<String> DICTIONARY = Arrays.asList("ola", "mundo", "sistemas", "distribuidos", "fcup", "tom", "lamport", "trofa", "porto", "ricardo");

    public Peer(String hostname, String portStr) {
        this.host = hostname;
        this.port = Integer.parseInt(portStr);
        this.id = hostname + ":" + this.port;
        
        // Inicializações fundamentais
        this.clock = new LamportClock(0);
        this.queue = new PriorityBlockingQueue<>();
       
        this.lastTimeStampFromPeer = new ConcurrentHashMap<>(); 
        
        try {
            this.logger = Logger.getLogger("logfile_" + id);
            FileHandler handler = new FileHandler("./" + host + "_" + port + "_peer.log", true);
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Uso: java Peer <host> <port> [<vizinho_host> <vizinho_port> ...]");
            return;
        }

        Peer peer = new Peer(args[0], args[1]);
        System.out.printf(">>> PEER INICIADO: %s <<<\n", peer.id);

        peer.allPeers = new PeerConnection();
        
        // Registar vizinhos e inicializar relógios
        for (int i = 2; i < args.length - 1; i += 2) {
            String nHost = args[i];
            int nPort = Integer.parseInt(args[i+1]);
            String nId = nHost + ":" + nPort;
            
            peer.allPeers.addVizinho(nPort, nHost);
            
            peer.lastTimeStampFromPeer.put(nId, 0); 
            System.out.println("Vizinho registado: " + nId);
        }

        TomServer server = new TomServer(peer);
        new Thread(server).start();

        TomSender sender = new TomSender(peer);
        new Thread(sender).start();
    }
    
    public void multicast(String type, String content, int timestamp) {
        Map<Integer, String> vizinhos = allPeers.getVizinhos();
        for (Map.Entry<Integer, String> entry : vizinhos.entrySet()) {
            sendMessage(entry.getValue(), entry.getKey(), type, content, timestamp);
        }
    }

    private void sendMessage(String targetHost, int targetPort, String type, String content, int timestamp) {
        try (Socket socket = new Socket(targetHost, targetPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(type + ":" + timestamp + ":" + this.id + ":" + content);
        } catch (Exception e) {
            System.out.println("[ERRO] Falha ao conectar a " + targetHost + ":" + targetPort);
        }
    }

    public synchronized void checkQueue() {
        while (!queue.isEmpty()) {
            Message msg = queue.peek();
            boolean isStable = true;
            
            // Verifica se todos os vizinhos já passaram do tempo da mensagem
            for (String peerId : lastTimeStampFromPeer.keySet()) {
                int lastTime = lastTimeStampFromPeer.get(peerId);
                if (lastTime <= msg.timestamp) {
                    isStable = false;
                    break;
                }
            }

            if (isStable) {
                queue.poll();
                System.out.println("[CHAT] " + msg.senderId + " diz: " + msg.content + " (T=" + msg.timestamp + ")");
            } else {
                break;
            }
        }
    }
}

class Message implements Comparable<Message> {
    int timestamp;
    String senderId;
    String content;

    public Message(int timestamp, String senderId, String content) {
        this.timestamp = timestamp;
        this.senderId = senderId;
        this.content = content;
    }

    @Override
    public int compareTo(Message other) {
        if (this.timestamp != other.timestamp) {
            return Integer.compare(this.timestamp, other.timestamp);
        }
        return this.senderId.compareTo(other.senderId);
    }
}

class TomServer implements Runnable {
    Peer peer;

    public TomServer(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(peer.port)) {
            while (true) {
                Socket client = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String line = in.readLine();
                if (line != null) handleMessage(line);
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(String line) {
        String[] parts = line.split(":", 4);
        if (parts.length < 3) return; 

        String type = parts[0];
        int msgTime = Integer.parseInt(parts[1]);
        String senderId = parts[2];
        String content = parts.length > 3 ? parts[3] : "";

        synchronized (peer) {
            peer.clock.setTime(Math.max(peer.clock.getTime(), msgTime) + 1);
            
            if (peer.lastTimeStampFromPeer.containsKey(senderId)) {
                peer.lastTimeStampFromPeer.put(senderId, msgTime);
            }

            if (type.equals("MSG")) {
                peer.queue.add(new Message(msgTime, senderId, content));
                peer.multicast("ACK", "ACK", peer.clock.getTime());
            }
            peer.checkQueue();
        }
    }
}

class TomSender implements Runnable {
    Peer peer;
    PoissonProcess poisson;

    public TomSender(Peer peer) {
        this.peer = peer;
        this.poisson = new PoissonProcess(0.2, new Random());
    }

    @Override
    public void run() {
        try {
            System.out.println("A aguardar 10 segundos para sincronização inicial...");
            Thread.sleep(10000); 
            System.out.println("A começar envio de mensagens!");

            while (true) {
                double waitSec = poisson.timeForNextEvent();
                Thread.sleep((long) (waitSec * 1000));
                
                String word = Peer.DICTIONARY.get(new Random().nextInt(Peer.DICTIONARY.size()));

                synchronized (peer) {
                    peer.clock.increment();
                    int time = peer.clock.getTime();
                    
                    peer.queue.add(new Message(time, peer.id, word));
                    peer.multicast("MSG", word, time);
                    System.out.println("[EU] Enviei: " + word + " (T=" + time + ")");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}