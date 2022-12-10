package mailservice.mail.mailserver;

import java.net.ServerSocket;
import java.net.Socket;

public class SmtpServer {
    public static void main(String[] args) {
        /* 서버 생성 */
        try {
            //서버 소켓을 생성하여 포트와 Binding
            ServerSocket serverSocket = new ServerSocket(7777);
            System.out.println("SmtpServer is Ready");


            /* 접속 요청 대기 */
            while (true) {
                try {
                    Socket socket = serverSocket.accept();  //접속요청 받음

                    /*smtpReceiver 실행하여 메일정보 전달받기*/
                    SmtpReceiver smtpReceiver = new SmtpReceiver(socket);
                    smtpReceiver.run();
                } catch (Exception e) {e.printStackTrace();}
            }
        } catch (Exception exception) {exception.printStackTrace();}
    }
}

