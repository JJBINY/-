package mailservice.mail.mailserver;

import mailservice.mail.Mail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

public class SmtpReceiver extends Thread {
    Socket socket;
    BufferedWriter out;
    BufferedReader in;
    String line;
    Mail mail;
    ArrayList<String> contents = new ArrayList<>();
    public SmtpReceiver(Socket socket) {
        this.socket = socket;
        mail = new Mail();
        try {
            in = new BufferedReader
                    (new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new BufferedWriter
                    (new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        } catch (Exception e) {
        }
    }

    @Override
    public void run() {
        /*메일 수신*/
        smtpReceive();

        /*메일서버 통해 메일 송신*/
        SmtpSender smtpSender = new SmtpSender(mail);
        smtpSender.start();
    }

    /*SMTP 프로토콜 수신측*/
    private void smtpReceive() {
        String command;
        try {
            System.out.println("connected with "+socket.getInetAddress()+"::"+socket.getPort());
            out.write("220 mail.joobin.site SMTP\r\n");
            out.flush();
        }catch (Exception e){}

        //일단은 무조건 프로토콜에 맞는 요청만 온다고 가정
        try {
            line = in.readLine();
            System.out.println("line = " + line);
            String[] strings = line.split(" ");
            System.out.println("strings = " + strings);

            //명령어만 파싱
            command = strings[0].replaceAll("(^\\p{Z}+|\\p{Z}+$)","");// replaceAll 부분은 공백제거임
            System.out.println("command = " + command);

            if ("HELO".equalsIgnoreCase(command.substring(0,4))) {
                out.write("250 mx.joobin.site at your service\r\n");
                out.flush();
            }
            while ((line = in.readLine()) != null) {
                System.out.println("line = " + line);
                strings = line.split("\\:");
                System.out.println("strings = " + strings);

                //명령어만 파싱
                command = strings[0].replaceAll("(^\\p{Z}+|\\p{Z}+$)","");// replaceAll 부분은 공백제거임
                System.out.println("command = " + command);

                if ("MAIL FROM".equalsIgnoreCase(command)){
                    mail.setFrom(strings[1].replaceAll("(^\\p{Z}+|\\p{Z}+$)",""));
                    out.write("250 2.1.0 OK" + "\r\n");
                    out.flush();
                }
                else if ("RCPT TO".equalsIgnoreCase(command)){
                    mail.setTo(strings[1].replaceAll("(^\\p{Z}+|\\p{Z}+$)",""));
                    out.write("250 2.1.5 OK" + "\r\n");
                    out.flush();
                }
                else if ("DATA".equalsIgnoreCase(command)){
                    out.write("354 Go ahead" + "\r\n");
                    out.flush();
                    while ((line = in.readLine()) != null){
                        if (line.equals("")){break;}
                        strings = line.split("\\:");
                        System.out.println("strings = " + strings);
                        //명령어만 파싱
                        command = strings[0].replaceAll("(^\\p{Z}+|\\p{Z}+$)","");// replaceAll 부분은 공백제거임
                        System.out.println("command = " + command);

                        /*이부분에서 헤더들 구분해서 입력(mail.setXXX)*/
                        if ("Subject".equalsIgnoreCase(command)){
                            mail.setSubject(strings[1]);
                        }
                        else if("from".equalsIgnoreCase(command)){
                            mail.setFromH(strings[1]);
                        }
                        else if("to".equalsIgnoreCase(command)){
                            mail.setToH(strings[1]);
                        }
                        else if("DATE".equalsIgnoreCase(command)){
                            mail.setDate(strings[1]);
                        }


                    }
                    System.out.println("End of Header");

                    /*바디 입력 : contents 입력*/
                    while ((line = in.readLine()) != null) {
                        if (line.equals(".")){break;}
                        System.out.println(line);
                        contents.add(line);
                    }
                    System.out.println("End of contents");
                    mail.setContents(contents);
                    out.write("250 2.0.0 OK"+"\r\n");
                    out.flush();
                }
                else if ("QUIT".equalsIgnoreCase(command)){
                    out.write("221 Closing Connection" + "\r\n");
                    out.flush();
                    socket.close();
                    break;
                }
                else {
                    out.write("502 5.5.1 Unrecognized command");
                    out.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                out.write("Exception! socket close");
                out.flush();
                socket.close();
                return;
            }catch (Exception exception){}
        }

        /*메일 정보 프린트*/
        System.out.println("Received Mail Print");
        System.out.println("mail.getSubject() = " + mail.getSubject());
        System.out.println("mail.getFrom() = " + mail.getFrom());
        System.out.println("mail.getTo() = " + mail.getTo());
        if (mail.getDate() == null) {
            mail.setDate(new Date().toString());
        }
        System.out.println("mail.getDate() = " + mail.getDate());
        for (String content : mail.getContents()) {
            System.out.println("content = " + content);
        }

        /*받은 메일 그대로 토스(전송)*/
        SmtpSender sender = new SmtpSender(mail);
//        sender.start();
    }
}
