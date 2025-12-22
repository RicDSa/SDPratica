package ds.assignment.tring;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;



// Serve para enviar o token para um peer ao inicio 
public class Token {

    static String token = "Token";


    private static void enviaToken(String host, int port){

        try {
            Socket socket = new Socket(host,port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println(token);
            out.flush();
            socket.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws Exception {
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        enviaToken(host,port);

        System.out.println("O token foi enviado com sucesso!");
    }
}