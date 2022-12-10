package mailservice.webserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import mailservice.webserver.HttpWorker;

/**
 * referance https://jojozhuang.github.io/programming/building-web-server-with-java-socket/
 * */
public class WebSocketServer {
    public static void main(String args[]){
        // The maximum queue length for incoming connection
        int queue_len = 6;
        // Port number for http request
        int port = 8080;
        // A reference of the client socket
        Socket socket;

        try{
            // Setup the server socket
            ServerSocket servsocket = new ServerSocket(port, queue_len);
            System.out.println("Web Server is starting up, listening at port " + port + ".");
            System.out.println("You can access http://localhost:"+port+" now.");

            while(true){
                // Make the server socket wait for the next client request
                socket = servsocket.accept();

                // Local reader from the client
                BufferedReader reader =new BufferedReader(new InputStreamReader(socket.getInputStream(),"utf-8"));


                // Assign http requests to HttpWorker
                String req = "";
                String clientRequest = "";
                int remaining = 0;
                /*요청메시지 한 줄씩 읽어옴; 헤더는 버림*/
                while ((clientRequest = reader.readLine()) != null) {
                    System.out.println("clientRequest = " + clientRequest); //전체 리퀘스트 메시지 출력
                    if (req.equals("")) {   //첫줄인 경우 req에 저장
                        req  = clientRequest;
                    }

                    if (clientRequest.equals("")) { // If the end of the http request, stop
                        break;
                    }
                }
                System.out.println("리퀘스트 메시지 첫번째 줄만 저장" +req);
                //읽어온 요청메시지와 소켓을 HttpWorker로 전달.
                if (req != null && !req.equals("")) {
                    new HttpWorker(req, socket).start();
                }

            }
        } catch(Exception e){
            //Handle the exception
            e.printStackTrace();}

        finally {
            System.out.println("Server has been shutdown!");
        }
    }
}