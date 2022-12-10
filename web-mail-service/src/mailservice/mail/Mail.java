package mailservice.mail;

import java.util.ArrayList;


public class Mail {

    private Long id = 0L;    // 메일 id
//    private Date date; //메일 발송일

    private String from = "test@joobin.site";   //보내는 사람 주소
    private String to = "sisoya0424@gmail.com";    //받는 사람 주소
    private String fromH = "test@joobin.site";   //From 헤더
    private String toH = "sisoya0424@gmail.com";    //To 헤더
    private String date = null;
    private String subject = "제목";
    private ArrayList<String> contents; //내용
//    private String messageId;
    public Mail(){}

    public Mail(String from, String to, String subject) {
        this.from = from;
        this.to = to;
        this.subject = subject;
//        this.date = new Date();
    }

    public Long getId() {
        return id;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getFromH() {
        return fromH;
    }

    public String getToH() {
        return toH;
    }

    public String getDate() {
        return date;
    }

    public String getSubject() {
        return subject;
    }

    public ArrayList<String> getContents() {
        return contents;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setFromH(String fromH) {
        this.fromH = fromH;
    }

    public void setToH(String toH) {
        this.toH = toH;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setContents(ArrayList<String> contents) {
        this.contents = contents;
    }
}