package com.o2.cz.cip.hashseek.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

/**
 * User: Pavel
 * Date: 8.11.12 14:11
 */
public class SeekParameters implements Comparable<SeekParameters> {

    public static enum SortBy {TIMESTAMP, USERNAME};
    public static enum SortDirection {ASC, DESC};
    static Logger LOGGER= LoggerFactory.getLogger(ProcessCommandHandler.class);    

    public static SortBy sortBy;
    public static SortDirection sortDirection;
    private String defect;
    private String seekDay;
    private String seekTime;
    private String hoursToSeek;
    private String seekString;
    private String seekString1;
    private String seekString2;
    private String session;
    private String fileName;
    private long timestamp;
    private long logonTime;
    private boolean isAuthenticated = false;
    private String userName = "not identified";
    private String userMail = "not identified";
    private String userPhone = "not identified";

    static Random sessionGenerator = new Random();
    static Calendar current = Calendar.getInstance();
    static Map<String, SeekParameters> seekParametersMap = new HashMap<String, SeekParameters>();

    public String getDefect() {
        return defect;
    }

    public void setDefect(String defect) {
        this.defect = defect;
    }

    public String getSeekDay() {
        return seekDay;
    }

    public void setSeekDay(String seekDay) {
        this.seekDay = seekDay;
    }

    public String getSeekTime() {
        return seekTime;
    }

    public void setSeekTime(String seekTime) {
        this.seekTime = seekTime;
    }

    public String getHoursToSeek() {
        return hoursToSeek;
    }

    public void setHoursToSeek(String hoursToSeek) {
        this.hoursToSeek = hoursToSeek;
    }

    public String getSeekString() {
        return seekString;
    }

    public String getSeekString1() {
        return seekString1;
    }

    public void setSeekString1(String seekString1) {
        this.seekString1 = seekString1;
    }

    public String getSeekString2() {
        return seekString2;
    }

    public void setSeekString2(String seekString2) {
        this.seekString2 = seekString2;
    }

    public void setSeekString(String seekString) {
        this.seekString = seekString;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }


    public static SeekParameters parseParameters(StringBuffer buf) throws UnsupportedEncodingException {
        String paramString = buf.toString();
        outPrintLine(String.format("Received: '%s'", paramString.replaceFirst("password=[^&]*", "password=XXXXX")));
        String session=null;
        String name=null;
        String password=null;
        String[] params = paramString.split("&");
        for (int i=0; i< params.length; i++) {
            String[] paramPair = params[i].split("=");
            if (paramPair.length > 1) {
                if ("session".equals(paramPair[0])) {
                    session = paramPair[1];
                }
            }
        }
        SeekParameters seekParameters = seekParametersMap.get(session);
        if (seekParameters == null) {
            seekParameters = new SeekParameters();
        } else {
            seekParameters.setDefect("");
            seekParameters.setSeekDay("");
            seekParameters.setSeekTime("");
            seekParameters.setHoursToSeek("");
            seekParameters.setSeekString("");
            seekParameters.setSeekString1("");
            seekParameters.setSeekString2("");
        }
        for (int i=0; i< params.length; i++) {
            String[] paramPair = params[i].split("=");
            if (paramPair.length > 1) {
                if ("defect".equals(paramPair[0])) {
                    seekParameters.setDefect(URLDecoder.decode(paramPair[1], "UTF-8"));
                } else if ("seekDay".equals(paramPair[0])) {
                    seekParameters.setSeekDay(URLDecoder.decode(paramPair[1], "UTF-8"));
                } else if ("seekTime".equals(paramPair[0])) {
                    seekParameters.setSeekTime(URLDecoder.decode(paramPair[1], "UTF-8"));
                } else if ("hoursToSeek".equals(paramPair[0])) {
                    seekParameters.setHoursToSeek(URLDecoder.decode(paramPair[1], "UTF-8"));
                } else if ("seekString".equals(paramPair[0])) {
                    seekParameters.setSeekString(URLDecoder.decode(paramPair[1], "UTF-8"));
                } else if ("seekString1".equals(paramPair[0])) {
                    seekParameters.setSeekString1(URLDecoder.decode(paramPair[1], "UTF-8"));
                } else if ("seekString2".equals(paramPair[0])) {
                    seekParameters.setSeekString2(URLDecoder.decode(paramPair[1], "UTF-8"));
                } else if ("session".equals(paramPair[0])) {
                    session = paramPair[1];
                } else if ("name".equals(paramPair[0])) {
                    name = URLDecoder.decode(paramPair[1], "UTF-8");
                } else if ("password".equals(paramPair[0])) {
                    password = URLDecoder.decode(paramPair[1], "UTF-8");
                }
            }
        }
        if (session == null) {
            session = getNewSessionIdentifier();
        }
        if (seekParameters.getDefect() == null || "".equals(seekParameters.getDefect().trim())) { //default pro oznaceni defektu.
            seekParameters.setDefect(session.substring(1,5));
        }
        SeekParameters existingSeekParameters = seekParametersMap.get(session);
        if (existingSeekParameters != null) {
            seekParameters.isAuthenticated = existingSeekParameters.isAuthenticated;
        }
        if (!seekParameters.isAuthenticated() && name != null && password !=null) {
           authenticate(name, password, seekParameters);
        }
        seekParameters.setSession(session);
        seekParameters.setTimestamp(System.currentTimeMillis()); //posledni cas aktivity

        seekParametersMap.put(session, seekParameters);
        return seekParameters;
    }

    static void outPrintLine(String line) {
        LOGGER.info(String.format("%s   %s", formatedDateTime(System.currentTimeMillis()), line));
    }

    static String formatedDateTime(long time) {
        current.setTimeInMillis(time);
        return String.format("%1$tY.%1$tm.%1$td %1$tH:%1$tM:%1$tS.%1$tL", current);
    }

    static String formatedTime(long time) {
        return String.format("%5d:%02d:%02d", time /60 /60, (time / 60) % 60, time % 60);
    }

    public static String getDifferenceTime(long lastTime) {
        long time = (System.currentTimeMillis() - lastTime)/1000;
        return formatedTime(time);
    }

    String getLastActivitySinceTime() {
        return SeekParameters.getDifferenceTime(timestamp);
    }

    String getLastActivityTime() {
        return SeekParameters.formatedDateTime(timestamp);
    }

    String getLogonTime() {
        if (isAuthenticated()) {
            return SeekParameters.formatedDateTime(logonTime);
        } else {
            return "";
        }
    }

    String getLogonSinceTime() {
        if (isAuthenticated()) {
            return SeekParameters.getDifferenceTime(logonTime);
        } else {
            return "";
        }
    }

    private static void authenticate(String domainUser , String password, SeekParameters seekParameters) {
        System.out.println("Starting authentication");
        try {
//            1	ntto201.to2.to2cz.cz	389	TO2\
//           2	ntto202.to2.to2cz.cz	389	TO2\
//           3	ntpha401.pha.user.ct.cz	389	PHA\
//           4	ntmor401.morava.user.ct.cz	389	USER\
//           5	ntsev401.sever.user.ct.cz	389	SEVER\
//           6	ntjih403.jih.user.ct.cz	389	USER\
//           7	ntusr401.user.ct.cz	389	USER\
//           8	NTAD403.ad.eurotel.cz	389	AD\
//

            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://10.21.136.10:389");
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, String.format("to2\\%s",domainUser));
            env.put(Context.SECURITY_CREDENTIALS, password);
            DirContext ctx = new InitialDirContext(env);
            if(ctx != null) {
                seekParameters.isAuthenticated = true;
                seekParameters.logonTime = System.currentTimeMillis();
                fillUser(ctx, domainUser, seekParameters);
                ctx.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
            System.out.println("End authentication with error");
            return;
        }
        System.out.println("End authentication OK");
    }


   private static void fillUser(DirContext ctx, String domainUser, SeekParameters seekParameters) throws Exception {
       System.out.println("Start fillUser");
       SearchControls constraints = new SearchControls();
       constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
       String[] attrIDs = {
               "givenname",
               "sn",
               "mail",
               "telephoneNumber"};
       constraints.setReturningAttributes(attrIDs);
       String name = "OU=usr,OU=acc,DC=to2,DC=to2cz,DC=cz";
       String matchingAttributes = String.format("cn=*%s*", domainUser.substring(2));
       NamingEnumeration<SearchResult> answer = ctx.search(name, matchingAttributes, constraints);
       if (answer.hasMore()) {
           Attributes attrs = ((SearchResult) answer.next()).getAttributes();
           Attribute att = attrs.get("givenname");
           if (att != null) {
               seekParameters.userName = String.format("%s",att.get());
           }
           att = attrs.get("sn");
           if (att != null) {
               seekParameters.userName = String.format("%s %s",seekParameters.userName, att.get());
           }
           att = attrs.get("mail");
           if (att != null) {
               seekParameters.userMail = String.format("%s", att.get());
           }
           att = attrs.get("telephoneNumber");
           if (att != null) {
               seekParameters.userPhone = String.format("%s", att.get());
           }
           outPrintLine(String.format("User info: name='%s', mail='%s', phone='%s' user='%s'", seekParameters.userName, seekParameters.userMail, seekParameters.userPhone, domainUser));
       }else{
           outPrintLine(String.format("No user info found in LDAP for ''", domainUser));
       }
       System.out.println("End fillUser");
   }

    static String getNewSessionIdentifier() {
        Long newSession;
        do {
            newSession = sessionGenerator.nextLong();
        } while (newSession < 100000);
        return String.format("%s",newSession);
    }

    public String getUserName() {
        return userName;
    }

    public String getUserMail() {
        return userMail;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public static void main(String[] args) {
        authenticate("x0534049","X", new SeekParameters());
    }

    private String getSortedKey() {
        if (SortBy.TIMESTAMP.equals(sortBy)) {
            return Long.toString(timestamp);
        } else if (SortBy.USERNAME.equals(sortBy)){
            return userName;
        } else { //default sort, no sort
            return "";
        }
    }

    public int compareTo(SeekParameters seekParameters) {
        if (SortDirection.ASC.equals(sortDirection)) {
            return getSortedKey().compareTo(seekParameters.getSortedKey());
        } else {
            return seekParameters.getSortedKey().compareTo(getSortedKey());
        }
    }

    public boolean showSession() {
        if ("pavel.novotny@o2.cz".equals(userMail)) {
            return true;
        } else {
            return false;
        }
    }

}
