package mailservice.mail.mailserver;


import mailservice.mail.Mail;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;

public class SmtpSender extends Thread {
    String ipAddress;
    String domainAddress;
    String domain = "gmail.com";
    String from = "<sisoya0424@joobin.site>";
    String to = "<sisoya0424@sju.ac.kr>";
    String localHost = "125.240.254.240";
    Integer port = 25;
    Boolean internalSend = false;
    private final Mail mail;

    public SmtpSender(Mail mail) {
        this.mail = mail;
    }

    public SmtpSender(Mail mail, Boolean internalSend) {
        this.internalSend = internalSend;
        this.mail = mail;

    }

    @Override
    public void run() {
        System.out.println("SmtpSender Start Running..");
        try {
            if (internalSend) {
                domainAddress = localHost;
                port = 7777;
                System.out.println("is InternalSned");
            } else {
                domain = mail.getTo().split("\\@")[1].replace(">", "");
                System.out.println("domain = " + domain);
                /*해당 도메인의 메일서버 주소를 domainAddress 객체로 읽어오기*/
                setDomainAddress(domain);
            }

            //tcp socket 연결하기

            /*25번 포트로 domainAddress와 TCP 통신 연결*/
            System.out.println("Sending Mail to "+domainAddress);
            Socket socket = new Socket(domainAddress, port);
//            SocketFactory factory = SSLSocketFactory.getDefault();
//            Socket socket = factory.createSocket(domainAddress, port);

            System.out.println("Socket access");

            /*입출력 버퍼 객체
             * UTF-8 형식의 문자를 버퍼를 통해 입출력
             * */
            BufferedReader in = new BufferedReader
                    (new InputStreamReader(socket.getInputStream(), "UTF-8"));
            BufferedWriter out = new BufferedWriter
                    (new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));


            /*통신 시작
             * SMTP 프로토콜에 따라 순서대로 입력
             * */
            send(in, out, "HELO world");    //world 부분에 아무거나 써도 상관없음
            send(in, out, "MAIL FROM: " + mail.getFrom());    //보내는 사람 이메일주소
            send(in, out, "RCPT TO: " + mail.getTo());    //받는사람 이메일 주소
            send(in, out, "DATA");  //여기서부터 메일 내용 작성하겠다는 뜻

            /*메일 헤더 작성 : 메일은 헤더와 바디로 구분됨; 모든 헤더가 필수인 것은 아님*/
            send(in, out, "Subject: " + mail.getSubject());   //메일 제목
            send(out, "From: " + mail.getFromH());   //보내는 사람
            send(out, "To: " + mail.getToH());   //받는 사람
            send(out, "Message-Id: <" + mail.getId() + "@joobin.site>");   //SPF인증용 아이디 (이건 신경안쓰셛 되요~)
            if (mail.getDate() != null) {
                send(out, "DATE: " + mail.getDate());
            } else {
                send(out, "DATE: " + new Date(System.currentTimeMillis()));  //날짜 - 필수 아님
            }
            send(out, "\r\n"); //헤더와 바디는 공백(\r\n)으로 구분됨

            // message body
            ArrayList<String> contents = mail.getContents();
            for (String content : contents) {
                send(out, content);  //메일 내용
            }
            send(out, "\r\n.\r\n");  //메일입력이 끝났다는 뜻 \r\n.\r\n라고 적히면 입력이 끝났다는 뜻이라고 약속을 정한것.

            send(in, out, "QUIT");  //통신종료
            send(in, out, "QUIT");  //통신종료 한번만 입력하면 버퍼 탓인지 제대로 전송 안되는 문제가 있어서 QUIT 2번 입력했음

            System.out.println(in.readLine());  //통신종료 메시지 읽어오기
            socket.close();  //소켓 닫음
            System.out.println("Socketclose");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setDomainAddress(String domain) {
        try {
            //MX ip Address 가져오기
            String[] mxDNS = getMX(domain);

            /*도메인 의 MX(메일)주소 읽어오는 코드 : 신경안쓰셔도 되요~*/
            for (String mx : mxDNS) {
                System.out.println(mx);

                InetAddress inetAddress = InetAddress.getByName(mx);
                System.out.println("inetAddress = " + inetAddress);


                String[] address = inetAddress.toString().split("/");
                domainAddress = address[0];
                System.out.println("domainAddress = " + domainAddress);
                ipAddress = address[1];
                System.out.println("ipAddress = " + ipAddress);
            }

            ipAddress = InetAddress.getByName(mxDNS[0]).toString().split("/")[1];   //메일(수신)서버 ip주소
            domainAddress = InetAddress.getByName(mxDNS[0]).toString().split("/")[0];   //메일(수신)서버 도메인주소
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 메세지 s를 out으로 상대방에게 전송함
     * 파라미터에 in, out 둘 다 있는 경우 in으로 응답을 읽고 out으로 s라는 문자 전송
     * out만 있을 경우 응답읽지 않고 out으로 s라는 문자 전송
     */
    public static void send(BufferedReader in, BufferedWriter out, String s) throws Exception {
        String line = in.readLine();    //응답 읽어오기
        System.out.println(line);   //읽어온 응답 메시지 출력

        out.write(s + "\r\n");    //s라는 문자 출력버퍼에 입력; /r/n은 엔터라고 생각
        out.flush();    //출력버퍼에 입력된 내용 전송
        System.out.println(s);  //출력으로 보낸 내용
    }

    public static void send(BufferedWriter out, String s) throws Exception{
        out.write(s + "\r\n");
        out.flush();
        System.out.println(s);
    }

    public static String[] getMX(String domainName) throws NamingException {
        Hashtable<String, Object> env = new Hashtable<String, Object>();

        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put(Context.PROVIDER_URL, "dns:");

        DirContext ctx = new InitialDirContext(env);
        Attributes attribute = ctx.getAttributes(domainName, new String[]{"MX"});
        Attribute attributeMX = attribute.get("MX");
        // if there are no MX RRs then default to domainName (see: RFC 974)
        if (attributeMX == null) {
            return (new String[]{domainName});
        }

        // split MX RRs into Preference Values(pvhn[0]) and Host Names(pvhn[1])
        String[][] pvhn = new String[attributeMX.size()][2];
        for (int i = 0; i < attributeMX.size(); i++) {
            pvhn[i] = ("" + attributeMX.get(i)).split("\\s+");
        }

        // sort the MX RRs by RR value (lower is preferred)
        Arrays.sort(pvhn, (o1, o2) -> Integer.parseInt(o1[0]) - Integer.parseInt(o2[0]));

        String[] sortedHostNames = new String[pvhn.length];
        for (int i = 0; i < pvhn.length; i++) {
            sortedHostNames[i] = pvhn[i][1].endsWith(".") ?
                    pvhn[i][1].substring(0, pvhn[i][1].length() - 1) : pvhn[i][1];
        }
        return sortedHostNames;
    }
}
