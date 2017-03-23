package com.o2.cz.cip.hashseek.http;

import com.o2.cz.cip.hashseek.app.AppProperties;
import com.o2.cz.cip.hashseek.app.CsvParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

/**
 * User: Pavel
 * Date: 8.11.12 14:11
 */
public class Session implements Comparable<Session> {
    static final Logger LOGGER= LoggerFactory.getLogger(Session.class);

    public boolean isNoeSeek() {
        return noeSeek;
    }

    public void setNoeSeek(boolean noeSeek) {
        this.noeSeek = noeSeek;
    }

    public boolean isEligibleNoeSeek() {
        return eligibleNoeSeek;
    }

    public void setEligibleNoeSeek(boolean eligibleNoeSeek) {
        this.eligibleNoeSeek = eligibleNoeSeek;
    }


    public static enum SortBy {TIMESTAMP, USERNAME};
    public static enum SortDirection {ASC, DESC};
    public static int SEEK_STRING_COUNT = 15;
    public static int AND_CONDITIONS = 3;
    public static int FILTER_STRING_COUNT = 3;
    public static int CSV_PARAM_COUNT = 3;
    public static int NAMESPACES_COUNT = 3;

    public static SortBy sortBy;
    public static SortDirection sortDirection;
    private String defect;
    private String seekDay;
    private String seekTime;
    private String hoursToSeek;
    private List<String> seekStrings;
    private String session;
    private String fileName;
    private long timestamp;
    private long logonTime;
    private boolean isAuthenticated = false;
    private String domainUser="";
    private String userName = "not identified";
    private String userMail = "not identified";
    private String userPhone = "not identified";
    private boolean predprod = false;
    private boolean prod = false;
    private boolean test = false;
    private boolean runinfo = false;
    private boolean onlineEsbSeek = false;
    private boolean onlineBPMSeek = false;
    private boolean b2bSeek = false;
    private boolean noeSeek = false;
    private boolean hashFileSeekOnly;
    private boolean includeTimeLogs;
    private int seekLimit = 100;
    private boolean eligibleSeekProd;
    private boolean eligibleSeekLimit;
    private boolean eligibleNoeSeek;
    private boolean eligibleShowSession;
    private long start = 0l;
    private List<String> filter;
    private List<CsvParams> csvParams;
    private Map<String, String> namespaces;
    private List<String> namespaceList;
    private List<String> prefixList;
    private boolean csvSeek=false;


    static Random sessionGenerator = new Random();
    static Calendar current = Calendar.getInstance();
    static Map<String, Session> sessionMap = new HashMap<String, Session>();

    public Session() {
        initializeSeekStrings();
        initializeFilterStrings();
        initializeCsvParams();
        initializeNamespaces();
    }

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

    public void setSeekString(int index, String seekString) {
        seekStrings.set(index, seekString);
    }

    public String getSeekString(int index) {
        return seekStrings.get(index);
    }

    public String getFilterString(int index) {
        return filter.get(index);
    }

    public List<String> getFilter() {
        return filter;
    }

    public void setFilter(int index, String filterString) {
        filter.set(index, filterString);
    }


    public void initializeFilterStrings() {
        filter = new ArrayList<String>(FILTER_STRING_COUNT);
        for (int i = 0; i < FILTER_STRING_COUNT; i++) {
            filter.add("");
        }
    }

    public List<CsvParams> getCsvParams() {
        return csvParams;
    }

    private void setCsvService(int i, String value) {
        CsvParams csvParam = csvParams.get(i);
        csvParam.setService(value);
    }

    private void setCsvRequest(int i, String value) {
        CsvParams csvParam = csvParams.get(i);
        csvParam.setRequest("on".equals(value));
    }

    private void setCsvXpath(int i, int j, String value) {
        csvParams.get(i).getxPath().set(j, value);
    }

    public void initializeCsvParams() {
        csvParams = new ArrayList<CsvParams>(CSV_PARAM_COUNT);
        for (int i = 0; i < CSV_PARAM_COUNT; i++) {
            csvParams.add(new CsvParams());
        }
    }

    private void setNamespace(int index, String prefix, String namespace) {
        namespaces.put(prefix, namespace);
        namespaceList.set(index, namespace);
        prefixList.set(index, prefix);
    }

    public void initializeNamespaces() {
        namespaces = new HashMap<String, String>();
        namespaceList = new ArrayList<String>();
        prefixList = new ArrayList<String>();
        for (int i = 0; i < NAMESPACES_COUNT; i++) {
            namespaceList.add("");
            prefixList.add("");
        }
    }

    public void initializeSeekStrings() {
        seekStrings = new ArrayList<String>(SEEK_STRING_COUNT);
        for (int i = 0; i < SEEK_STRING_COUNT; i++) {
            seekStrings.add("");
        }
    }

    public boolean isSeekStringsEmpty() {
        for (int i = 0; i < SEEK_STRING_COUNT; i++) {
            String seekString = seekStrings.get(i);
            if (seekString != null && !"".equals(seekString.trim())) {
                return false;
            }
        }
        return true;
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


    public static Session parseParameters(StringBuffer buf) throws UnsupportedEncodingException {
        String paramString = buf.toString();
        outPrintLine(String.format("Received: '%s'", paramString.replaceFirst("password=[^&]*", "password=XXXXX")));
        String sessionId=null;
        String name=null;
        String password=null;
        String[] params = paramString.split("&");
        for (int i=0; i< params.length; i++) {
            String[] paramPair = params[i].split("=");
            if (paramPair.length > 1) {
                if ("session".equals(paramPair[0])) {
                    sessionId = paramPair[1];
                }
            }
        }
        Session session = sessionMap.get(sessionId);
        if (session == null) {
            session = new Session();
        } else {
            session.setDefect("");
            session.setSeekDay("");
            session.setSeekTime("");
            session.setHoursToSeek("");
            session.initializeSeekStrings();
            session.initializeFilterStrings();
            session.initializeCsvParams();
            session.initializeNamespaces();
        }
        session.setTimestamp(System.currentTimeMillis() - 400); //posledni cas aktivity, korekce protoze timestamp souboru nehraje java casem
        session.setPredprod(paramString.contains("predprod=on"));
        session.setProd(paramString.contains("produkce=on"));
        session.setTest(paramString.contains("test=on"));
        session.setRuninfo(paramString.contains("runinfo=on"));
        session.setOnlineEsbSeek(paramString.contains("onlineconnectesb=on"));
        session.setB2bSeek(paramString.contains("b2bseek=on"));
        session.setNoeSeek(paramString.contains("noeseek=on"));
        session.setOnlineBPMSeek(paramString.contains("onlineconnectbpm=on"));
        session.setHashFileSeekOnly(paramString.contains("seekhashesonly=on"));
        session.setIncludeTimeLogs(paramString.contains("includetimelogs=on"));
        session.setCsvSeek(paramString.contains("csvseek=on"));
        HashMap<String, String> parameterMap = new HashMap<String, String>();
        for (int i=0; i< params.length; i++) {
            String[] paramPair = params[i].split("=");
            parameterMap.put(paramPair[0],paramPair.length>1?URLDecoder.decode(paramPair[1], "UTF-8"):"");
        }
        session.setDefect(parameterMap.get("defect"));
        session.setSeekDay(parameterMap.get("seekDay"));
        session.setSeekLimit(makeInt(parameterMap.get("seekLimit"), session.getSeekLimit()));
        session.setSeekTime(parameterMap.get("seekTime"));
        session.setHoursToSeek(parameterMap.get("daysToSeek"));
        session.setSeekString(0, parameterMap.get("seekString0"));
        session.setSeekString(1, parameterMap.get("seekString1"));
        session.setSeekString(2, parameterMap.get("seekString2"));
        session.setSeekString(3, parameterMap.get("seekString3"));
        session.setSeekString(4, parameterMap.get("seekString4"));
        session.setSeekString(5, parameterMap.get("seekString5"));
        session.setSeekString(6, parameterMap.get("seekString6"));
        session.setSeekString(7, parameterMap.get("seekString7"));
        session.setSeekString(8, parameterMap.get("seekString8"));
        session.setSeekString(9, parameterMap.get("seekString9"));
        session.setSeekString(10, parameterMap.get("seekString10"));
        session.setSeekString(11, parameterMap.get("seekString11"));
        session.setSeekString(12, parameterMap.get("seekString12"));
        session.setSeekString(13, parameterMap.get("seekString13"));
        session.setSeekString(14, parameterMap.get("seekString14"));
        session.setFilter(0, parameterMap.get("filterString0"));
        session.setFilter(1, parameterMap.get("filterString1"));
        session.setFilter(2, parameterMap.get("filterString2"));
        session.setCsvService(0, parameterMap.get("csvService0"));
        session.setCsvService(1, parameterMap.get("csvService1"));
        session.setCsvService(2, parameterMap.get("csvService2"));
        session.setCsvRequest(0, parameterMap.get("csvRequest0"));
        session.setCsvRequest(1, parameterMap.get("csvRequest1"));
        session.setCsvRequest(2, parameterMap.get("csvRequest2"));
        session.setCsvXpath(0, 0, parameterMap.get("csvXpath00"));
        session.setCsvXpath(0, 1, parameterMap.get("csvXpath01"));
        session.setCsvXpath(1, 0, parameterMap.get("csvXpath10"));
        session.setCsvXpath(1, 1, parameterMap.get("csvXpath11"));
        session.setCsvXpath(2, 0, parameterMap.get("csvXpath20"));
        session.setCsvXpath(2, 1, parameterMap.get("csvXpath21"));
        session.setNamespace(0, parameterMap.get("nsPrefix0"), parameterMap.get("namespace0"));
        session.setNamespace(1, parameterMap.get("nsPrefix1"), parameterMap.get("namespace1"));
        session.setNamespace(2, parameterMap.get("nsPrefix2"), parameterMap.get("namespace2"));
        sessionId = parameterMap.get("session");
        name = parameterMap.get("name");
        password = parameterMap.get("password");

        if (sessionId == null) {
            sessionId = getNewSessionIdentifier();
        }
        if (session.getDefect() == null || "".equals(session.getDefect().trim())) { //default pro oznaceni defektu.
            session.setDefect(sessionId.substring(1, 5));
        }
        outPrintLine(String.format("%s kontrola autentikace", sessionId));
        Session existingSession = sessionMap.get(sessionId);
        if (existingSession != null) {
            outPrintLine(String.format("%s session exists.", sessionId));
            session.isAuthenticated = existingSession.isAuthenticated;
        }
        outPrintLine(String.format("%s isAuthenticated %s",sessionId, session.isAuthenticated()));
        outPrintLine(String.format("%s name %s",sessionId, name));
        outPrintLine(String.format("%s password is %s",sessionId, password !=null?"not null":"null"));
        if (!session.isAuthenticated() && name != null && password !=null) {
            outPrintLine(String.format("%s going to authenticate O2",sessionId));
            authenticate(true, name, password, session);
            if (!session.isAuthenticated) {
                outPrintLine(String.format("%s going to authenticate Cetin",sessionId));
                authenticate(false, name, password, session);
            }
           authorize(name, session);
        }
        session.setSession(sessionId);

        sessionMap.put(sessionId, session);
        return session;
    }

    private static int makeInt(String s, int errorValue) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return errorValue;
        }
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
        return Session.getDifferenceTime(timestamp);
    }

    String getLastActivityTime() {
        return Session.formatedDateTime(timestamp);
    }

    String getLogonTime() {
        if (isAuthenticated()) {
            return Session.formatedDateTime(logonTime);
        } else {
            return "";
        }
    }

    String getLogonSinceTime() {
        if (isAuthenticated()) {
            return Session.getDifferenceTime(logonTime);
        } else {
            return "";
        }
    }

    private static void authenticate(boolean to2, String domainUser , String password, Session session) {
        LOGGER.info("Starting authentication");
        if (password == null || "".equals(password.trim())) {
            return;
        }
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
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            if (to2) {
                env.put(Context.PROVIDER_URL, "ldap://10.21.136.10:389");
                env.put(Context.SECURITY_PRINCIPAL, String.format("to2\\%s", domainUser));
            } else {
                env.put(Context.PROVIDER_URL, "ldap://172.23.168.6:3268");
                env.put(Context.SECURITY_PRINCIPAL, String.format("%s@cetin.cz", domainUser));
            }
            env.put(Context.SECURITY_CREDENTIALS, password);
            DirContext ctx = new InitialDirContext(env);
            if(ctx != null) {
                session.isAuthenticated = true;
                session.logonTime = System.currentTimeMillis();
                session.setDomainUser(domainUser);
                fillUser(ctx, domainUser, session);
                ctx.close();
            }
        }
        catch (Exception e) {
            LOGGER.info("Chyba přihlášení", e);
            return;
        }
    }

   private static void authorize(String domainUser, Session session) {
       session.setEligibleSeekProd(AppProperties.isProdEligible(domainUser));
       session.setEligibleSeekLimit(AppProperties.isSeekLimitEligible(domainUser));
       session.setEligibleNoeSeek(AppProperties.isNoeSeekEligible(domainUser));
       session.setEligibleShowSession(AppProperties.isSeekShowSessionEligible(domainUser));
   }

   private static void fillUser(DirContext ctx, String domainUser, Session session) throws Exception {
       LOGGER.info("Starting fillUser");
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
               session.userName = String.format("%s",att.get());
           }
           att = attrs.get("sn");
           if (att != null) {
               session.userName = String.format("%s %s",session.userName, att.get());
           }
           att = attrs.get("mail");
           if (att != null) {
               session.userMail = String.format("%s", att.get());
           }
           att = attrs.get("telephoneNumber");
           if (att != null) {
               session.userPhone = String.format("%s", att.get());
           }
           outPrintLine(String.format("%s User info: name='%s', mail='%s', phone='%s' user='%s'", session.getSession(), session.userName, session.userMail, session.userPhone, domainUser));
       }else{
           outPrintLine(String.format("%s No user info found in LDAP for '%s'", session.getSession(), domainUser));
       }
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
        Session session = new Session();
        //authenticate(false,"x0551013","xx", session);
        //authenticate(false,"x0551013","", session);
        //System.out.println(String.format("%s %s", session.getDomainUser(), session.getUserName()));
        //System.out.println(String.format("is authenticated %s", session.isAuthenticated?"true":"false"));
        authenticate(true,"x0534049",null, session);
        System.out.println(String.format("%s %s", session.getDomainUser(), session.getUserName()));
        System.out.println(String.format("is authenticated %s", session.isAuthenticated?"true":"false"));
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

    public int compareTo(Session session) {
        if (SortDirection.ASC.equals(sortDirection)) {
            return getSortedKey().compareTo(session.getSortedKey());
        } else {
            return session.getSortedKey().compareTo(getSortedKey());
        }
    }

    public boolean isPredprod() {
        return predprod;
    }

    public void setPredprod(boolean predprod) {
        this.predprod = predprod;
    }

    public boolean isProd() {
        return prod;
    }

    public void setProd(boolean prod) {
        this.prod = prod;
    }

    public boolean isRuninfo() {
        return runinfo;
    }

    public void setRuninfo(boolean runinfo) {
        this.runinfo = runinfo;
    }

    public boolean isTest() {
        return test;
    }

    public boolean isOnlineEsbSeek() {
        return onlineEsbSeek;
    }

    public boolean isB2bSeek() {
        return b2bSeek;
    }

    public void setB2bSeek(boolean b2bSeek) {
        this.b2bSeek = b2bSeek;
    }


    public boolean isOnlineBPMSeek() {
        return onlineBPMSeek;
    }

    public void setOnlineEsbSeek(boolean onlineEsbSeek) {
        this.onlineEsbSeek = onlineEsbSeek;
    }

    public void setOnlineBPMSeek(boolean onlineBPMSeek) {
        this.onlineBPMSeek = onlineBPMSeek;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public boolean isHashFileSeekOnly() {
        return hashFileSeekOnly;
    }

    public void setHashFileSeekOnly(boolean hashFileSeekOnly) {
        this.hashFileSeekOnly = hashFileSeekOnly;
    }

    public boolean isIncludeTimeLogs() {
        return includeTimeLogs;
    }

    public void setIncludeTimeLogs(boolean includeTimeLogs) {
        this.includeTimeLogs = includeTimeLogs;
    }

    public String getDomainUser() {
        return domainUser;
    }

    public void setDomainUser(String domainUser) {
        this.domainUser = domainUser;
    }

    public int getSeekLimit() {
        return seekLimit;
    }

    public void setSeekLimit(int seekLimit) {
        this.seekLimit = seekLimit;
    }

    public boolean isEligibleSeekProd() {
        return eligibleSeekProd;
    }

    public void setEligibleSeekProd(boolean eligibleSeekProd) {
        this.eligibleSeekProd = eligibleSeekProd;
    }

    public boolean isEligibleSeekLimit() {
        return eligibleSeekLimit;
    }

    public void setEligibleSeekLimit(boolean eligibleSeekLimit) {
        this.eligibleSeekLimit = eligibleSeekLimit;
    }

    public boolean isEligibleShowSession() {
        return eligibleShowSession;
    }

    public void setEligibleShowSession(boolean eligibleShowSession) {
        this.eligibleShowSession = eligibleShowSession;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getStart() {
        return start;
    }

    public List<String> getNamespaceList() {
        return namespaceList;
    }

    public List<String> getPrefixList() {
        return prefixList;
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public boolean isCsvSeek() {
        return csvSeek;
    }

    public void setCsvSeek(boolean csvSeek) {
        this.csvSeek = csvSeek;
    }
}
