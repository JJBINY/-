package mailservice.webserver;

import mailservice.mail.Mail;
import mailservice.mail.mailserver.SmtpSender;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

class HttpWorker extends Thread {

    Socket socket;
    String clientRequest;

    /**전달바은 인자를 저장(생성자)
     * Construct
     * @param s, the socket which is to be monitored
     */
    public HttpWorker (String req, Socket s)
    {
        socket = s;
        clientRequest = req;
    }

    /**
     * Start to work, after being assigned tasks by the server
     */
    public void run(){
        try{
            // Clear list each time for handling new request
            LogUtil.clear();    //logUtil은 로그 찍어주는 클래스
            // Local reader from the client
//            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Output stream to the client
            PrintStream printer = new PrintStream(socket.getOutputStream());


            LogUtil.write("");
            LogUtil.write("Http Worker is working...");
            LogUtil.write(clientRequest);

            /*
            GET으로 시작하지 않거나
            요청메시지 길이가 너무 짧거나(왜 14이하로 정해놨는지는 알아봐야 할 거 같습니다)
            HTTP/1.0으로 끝나지 않거나
            HTTP/1.1로 끝나는 경우
            400 Bad Request
             */
            if ((!clientRequest.startsWith("GET") && !clientRequest.startsWith("POST"))
                    || clientRequest.length() < 14 ||
                    !(clientRequest.endsWith("HTTP/1.0") || clientRequest.endsWith("HTTP/1.1"))
            ) {
                // bad request
                LogUtil.write("400(Bad Request): " + clientRequest);
                String errorPage = buildErrorPage("400", "Bad Request", "Your browser sent a request that this server could not understand.");
                printer.println(errorPage);

            }
            else {
                System.out.println("파싱 전 = " + clientRequest);
                String req = clientRequest.substring(4, clientRequest.length()-9).trim();   //요청 받은 메소드와 파일경로만 파싱
                System.out.println("파싱 후 요청받은 파일경로만 남김= " + req);

                if (req.indexOf("..") > -1 || req.indexOf("/.ht") > -1 || req.endsWith("~")) {
                    // hack attack
                    LogUtil.write("403(Forbidden): " + req);
                    String errorPage = buildErrorPage("403", "Forbidden", "You don't have permission to access the requested URL " + req);
                    printer.println(errorPage);
                }
                else {
                    // Remove the last slash if exists
                    if (req.endsWith("/")) {
                        req = req.substring(0, req.length() - 1);
                    }

                    System.out.println("디코딩 전 = " + req);
                    // Decode url, eg. New%20folder -> New folder
                    req = URLDecoder.decode(req, "UTF-8");
                    System.out.println("디코딩 후= " + req);

                    /*쿼리가 존재하면 데이터 토대로 메일 보냄*/
                    if (req.contains("?")){
                        req = writeMail(req);
                    }

                    callRequestedFile(printer, req);
                }
            }
            // Save logs to file
            LogUtil.save(true);
            socket.close();
        }
        catch(Exception e){e.printStackTrace();}
    }

    private static String writeMail(String req) {
        //                        System.out.println("쿼리가 존재함");
        String[] strings = req.split("\\?");

        String[] querys = strings[1].split("\\&");
        req = strings[0];

        /*쿼리를 키 벨류 쌍으로 매핑*/
        HashMap<String, String> map = new HashMap<>();
        for (String query : querys) {
            String[] ss = query.split("\\=");

            if (query.endsWith("=")){ //일종의 예외처리
                map.put(ss[0],"Empty Value");
                continue;}
            map.put(ss[0],ss[1]);   //key, value 매핑
        }

        /*메일 객체 작성*/
        String s;
        Mail mail = new Mail();
        for (String key : map.keySet()) {
            switch (key){
                case "to":
                    s = map.get(key);
                    /*"<>"추가*/
                    if (!s.contains("<")){
                        s = "<".concat(s);
                    }
                    if (!s.contains(">")){
                        s = s.concat(">");
                    }
                    mail.setTo(s);
                    mail.setToH(s);
                    break;
                case "from":
                    s = map.get(key);
                    /*"<>"추가*/
                    if (!s.contains("<")){
                        s = "<".concat(s);
                    }
                    if (!s.contains(">")){
                        s = s.concat(">");
                    }

                    mail.setFrom(s);
                    mail.setFromH(s);
                    break;
                case "subject":
                    mail.setSubject(map.get(key));
                    break;
                case "contents":
                    ArrayList<String> arrayList = new ArrayList<>();
                    String[] contents = map.get(key).split("\\n");
                    for (String content : contents) {
                        System.out.println("content = " + content);
                        arrayList.add(content);
                    }
                    mail.setContents(arrayList);
                    break;
            }
        }//mail 객체 작성 완료

        /*내부 메일 서버로 메일 전달*/
        new SmtpSender(mail,true).start();
        return req;
    }

    private void callRequestedFile(PrintStream printer, String req) throws Exception {
        /*Default("") 요청 index.html 요청으로 받아들임*/
        if(req.equals("")|| req.endsWith("/index.html")){
            req ="src/resources/static/index.html";
            LogUtil.write("> This is a [Default] request..");
            handleRequest(req, printer);

        }
        /*메일 등록 폼 요청*/
        else if(req.equals("/basic/mailForm")){
            req = "src/resources/templates" + req +".html";
            LogUtil.write("> This is a [MailForm] request..");
            handleRequest(req, printer);

        }
        /*메일 등록 성공 폼 요청*/
        else if(req.equals("/basic/mailOK")){
            req = "src/resources/templates" + req +".html";
            LogUtil.write("> This is a [SendingMailSuccessForm] request..");
            handleRequest(req, printer);
        }
        else if(req.startsWith("/css")){
            req = "src/resources/templates" + req;
            LogUtil.write("> This is a [css] request..");
            handleRequest(req, printer);

        }
        else { //요청된 페이지 찾지 못했으므로 404 Not Found로 응답
            LogUtil.write("404(NotFound): " + req);
            String errorPage = buildErrorPage("404", "NotFound", "the requested URL" + req + " was not found.");
            printer.println(errorPage);

        }
    }


    /**
     * 웰컴 페이지(index.html) 요청에 대한 처리(응답 메시지 작성)
     * @param req
     * @param printer
     */
    private void handleRequest(String req, PrintStream printer) throws Exception{
        /*요청받는 파일 경로*/

        String rootDir = getRootFolder();// Get the root folder of the webserver
        String path = Paths.get(rootDir, req).toString();// Get the real file path
        System.out.println("path = " + path);
        // Try to open the directory
        File file = new File (path) ;
        if (!file.exists()) { // If the directory does not exist
            printer.println("No such resource:" + req);
            

            LogUtil.write(">> No such resource:" + req);
        }
        else { // If exists

            LogUtil.write(">> Rendering contents of the file: " + file.getName());
            // Get all the files and directory under current directory

            /*HTML 작성 시작*/
            // Build file/directory structure in html format
            StringBuilder sbHtml = new StringBuilder();  //StringBuilder로 문자열을 이어붙여줌(문자열을 +로 붙여주는 것과 유사하지만 좀 더 효율적; 중간 객체 생성이 안됨)
            sbHtml.append(readFileInPath(path));    //file내용 읽어와 String객체로 저장


            String htmlHeader = buildHttpHeader(path, sbHtml.toString().getBytes().length);   //HTTP 헤더 작성
//            String htmlPage = buildHtmlPage(sbHtml.toString(), "");  //HTML 페이지 작성 완료
//            String htmlHeader = buildHttpHeader(path, htmlPage.length());   //HTTP 헤더 작성
            
            printer.println(htmlHeader);    //HTTP 응답헤더 출력
            printer.println(sbHtml);  //HTTP 바디(HTML메시지) 출력
            System.out.println("Message Responsed\n\n");
        }
    }


    /**
     * 해당 path 파일 찾아서 내용 읽어오기
     * @param filePath
     * @return
     * @throws Exception
     */
    public static String readFileInPath(String filePath) throws Exception{
        File file = new File(filePath);     //읽어올 파일 객체 생성
        StringBuilder sbHtml = new StringBuilder();     //읽어온 파일 내용 String으로 저장할 객체


        String fileLine = "";
        BufferedReader br = new BufferedReader(new FileReader(file));       //버퍼리더로 읽음으로써 바이트가 아닌 인코딩된 정보 읽어옴

        while ((fileLine = br.readLine()) != null) {        //한줄씩 파일 내용 읽어옴
            sbHtml.append(fileLine);        // 파일 내용 String 형식으로 저장
//            if(!filePath.endsWith("css"))
//                System.out.println(fileLine);   //콘솔에 출력;테스트용
        }
        return sbHtml.toString();       //저장된 String 반환
    }

    /**
     * Build http header
     * @param path, path of the request
     * @param length, length of the content
     * @return, header text
     */
    private String buildHttpHeader(String path, long length) {
        StringBuilder sbHtml = new StringBuilder();
        sbHtml.append("HTTP/1.1 200 OK");
        sbHtml.append("\r\n");
        sbHtml.append("Content-Length: " + length);
        sbHtml.append("\r\n");
        sbHtml.append("Content-Type: "+ getContentType(path));
        sbHtml.append("\r\n");
        return sbHtml.toString();
    }


    /**
     * Build error page for bad request
     * @param code, http cde: 400, 301, 200, 404
     * @param title, page title
     * @param msg, error message
     * @return, page text
     */
    private String buildErrorPage(String code, String title, String msg) {
        StringBuilder sbHtml = new StringBuilder();
        sbHtml.append("HTTP/1.1 " + code + " " + title + "\r\n\r\n");
        sbHtml.append("<!DOCTYPE html>");
        sbHtml.append("<html>");
        sbHtml.append("<head>");
        sbHtml.append("<title>" + code + " " + title + "</title>");
        sbHtml.append("</head>");
        sbHtml.append("<body>");
        sbHtml.append("<h1>" + code + " " + title + "</h1>");
        sbHtml.append("<p>" + msg + "</p>");
        sbHtml.append("<hr>");
        sbHtml.append("<p>*This page is returned by Web Server.</p>");
        sbHtml.append("</body>");
        sbHtml.append("</html>");
        return sbHtml.toString();
    }

    /**
     * Get file or directory list
     * @param filelist, original file/directory list
     * @param isfolder, flag indicates looking for file or directory list
     * @return, file/directory list
     */
    private List<File> getFileByType(File[] filelist, boolean isfolder) {
        List<File> files = new ArrayList<File>();
        if (filelist == null || filelist.length == 0) {
            return files;
        }

        for (int i = 0; i < filelist.length; i++) {
            if (filelist[i].isDirectory() && isfolder) {
                files.add(filelist[i]);
            }
            else if (filelist[i].isFile() && !isfolder) {
                files.add(filelist[i]);
            }
        }
        return files;
    }

    /**
     * Parse parameter from url to key value pair
     * @param url, url from client
     * @return, pair list
     */
    private Map<String, String> parseUrlParams(String url) throws UnsupportedEncodingException {
        HashMap<String, String> mapParams = new HashMap<String, String>();
        if (url.indexOf("?") < 0) {
            return mapParams;
        }

        url = url.substring(url.indexOf("?") + 1);
        String[] pairs = url.split("&");
        for (String pair : pairs) {
            int index = pair.indexOf("=");
            mapParams.put(URLDecoder.decode(pair.substring(0, index), "UTF-8"), URLDecoder.decode(pair.substring(index + 1), "UTF-8"));
        }
        return mapParams;
    }

    /**
     * Get root path
     * @return, path of the current location
     */
    private String getRootFolder() {
        String root = "";
        try{
            File f = new File(".");
            root = f.getCanonicalPath();
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
        return root;
    }

    /**
     * Get MIME type according to file extension
     * @param path, file path
     * @return, MIME type
     */
    private static String getContentType(String path) {
        if (path == null || path.equals("") || path.lastIndexOf(".") < 0) {
            return "text/html";
        }

        String extension = path.substring(path.lastIndexOf("."));
        switch(extension) {
            case ".html":
            case ".htm":
                return "text/html";
            case ".txt":
                return "text/plain";
            case ".ico":
                return "image/x-icon .ico";
            case ".wml":
                return "text/html"; //text/vnd.wap.wml
            case ".css":
                return "text/css";
            default:
                return "text/plain";
        }
    }
}
