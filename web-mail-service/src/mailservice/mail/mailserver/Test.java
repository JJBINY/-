package mailservice.mail.mailserver;

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
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;

public class Test {
    public static void main(String[] args) {
        String ipAddress;
        String domainAddress;
        String from = "<sisoya0424@naver.com>";
        String to = "<sisoya0424@gmail.com>";
//        String localHost =  "192.168.219.102";
        String localHost =  "192.168.20.61";

        //MX ip Address 가져오기
        try {
            String[] mxDNS = getMX("gmail.com");

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

            //tcp socket 연결하기
            try {
                /*25번 포트로 domainAddress와 TCP 통신 연결*/
                domainAddress =localHost;//Test
                System.out.println("connecting server...");
                Socket socket = new Socket(domainAddress, 7777);

                System.out.println("Socket access");

                /*입출력 버퍼 객체*/
                BufferedReader in = new BufferedReader
                        (new InputStreamReader(socket.getInputStream(), "UTF-8"));
                BufferedWriter out = new BufferedWriter
                        (new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

                /*통신 시작
                * SMTP 프로토콜에 따라 순서대로 입력
                * */
                send(in, out, "helo host");    //world 부분에 아무거나 써도 상관없음
                send(in, out, "MAIL FROM: " + from);    //보내는 사람 이메일주소
                send(in, out, "RCPT TO: " + to);    //받는사람 이메일 주소
                send(in, out, "DATA");  //여기서부터 메일 내용 작성하겠다는 뜻

                /*메일 헤더 작성 : 메일은 헤더와 바디로 구분됨; 헤더가 필수인 것은 아님*/
                send(in, out, "Subject: "+ "테스트 메일 제목");   //메일 제목
                send(out, "From: "+from);   //보내는 사람
                send(out, "To: "+to);   //받는 사람
                send(out, "Message-Id: <20221abcdefg@joobin.site>");   //SPF인증용 아이디 (이건 신경안쓰셛 되요~)
                send(out,"DATE: " + new Date(System.currentTimeMillis()));  //날짜
                send (out, "\r\n"); //헤더와 바디는 공백으로 구분됨
                // message body
                send(out, "test whole sending process");  //메일 내용
                send(out,"second line of contents");
                send(out,"Last line 한글 테스트");
                send(out,"\r\n.\r\n");  //메일입력이 끝났다는 뜻 \r\n.\r\n라고 적히면 입력이 끝났다는 뜻이라고 약속을 정한것.

                send(in, out, "QUIT");  //통신종료
                send(in, out, "QUIT");  //통신종료

                System.out.println(in.readLine());
                socket.close();  //소켓 닫음
                System.out.println("Socketclose");
            } catch (Exception e) {e.printStackTrace();}

        }catch (Exception e){e.printStackTrace();}
    }

    /**
     * 메세지 s를 out으로 상대방에게 전송함
     * */
    public static void send(BufferedReader in, BufferedWriter out, String s) {
        try {
            String line = in.readLine();

            System.out.println(line);
            out.write(s + "\r\n");
            out.flush();
            System.out.println(s);

        }catch (Exception e){e.printStackTrace();}
    }
    public static void send(BufferedWriter out, String s) {
        try {
            out.write(s + "\r\n");
            out.flush();
            System.out.println(s);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String[] getMX(String domainName) throws NamingException {
        Hashtable<String, Object> env = new Hashtable<String, Object>();

        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put(Context.PROVIDER_URL, "dns:");

        DirContext ctx = new InitialDirContext(env);
        Attributes attribute = ctx.getAttributes(domainName, new String[] {"MX"});
        Attribute attributeMX = attribute.get("MX");
        // if there are no MX RRs then default to domainName (see: RFC 974)
        if (attributeMX == null) {
            return (new String[] {domainName});
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
