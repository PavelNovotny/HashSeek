package com.o2.cz.cip.hashseek.blockseek;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.xml.sax.SAXException;

/**
 * Created by pavelnovotny on 25.05.15.
 */
public class XPathUtil {

    public static void main(String[] args) throws IOException, SAXException, XPathExpressionException {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        XPath xPath =  XPathFactory.newInstance().newXPath();
        String expression = "/employees/employee[@emplid='3333']/email";
        String xml =
                "<employees>\n" +
                "    <employee emplid=\"1111\" type=\"admin\">\n" +
                "        <firstname>John</firstname>\n" +
                "        <lastname>Watson</lastname>\n" +
                "        <age>30</age>\n" +
                "        <email>johnwatson@sh.com</email>\n" +
                "    </employee>\n" +
                "    <employee emplid=\"2222\" type=\"admin\">\n" +
                "        <firstname>Sherlock</firstname>\n" +
                "        <lastname>Homes</lastname>\n" +
                "        <age>32</age>\n" +
                "        <email>sherlock@sh.com</email>\n" +
                "    </employee>\n" +
                "    <employee emplid=\"3333\" type=\"user\">\n" +
                "        <firstname>Jim</firstname>\n" +
                "        <lastname>Moriarty</lastname>\n" +
                "        <age>52</age>\n" +
                "        <email>jim@sh.com</email>\n" +
                "    </employee>\n" +
                "    <employee emplid=\"4444\" type=\"user\">\n" +
                "        <firstname>Mycroft</firstname>\n" +
                "        <lastname>Holmes</lastname>\n" +
                "        <age>41</age>\n" +
                "        <email>mycroft@sh.com</email>\n" +
                "    </employee>\n" +
                "</employees>";
        Document xmlDocument = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        String email = xPath.compile(expression).evaluate(xmlDocument);
        System.out.println(String.format("Email %s",email));
    }


    public static String xPath(String xPathExpr, String xml, Map<String, String> namespaces) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        XPath xPath =  XPathFactory.newInstance().newXPath();
        Document xmlDocument = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        NameSpaceCtx nc = new NameSpaceCtx();
        nc.setNamespace(namespaces);
        xPath.setNamespaceContext(nc);
        return xPath.compile(xPathExpr).evaluate(xmlDocument);
    }

    public static class NameSpaceCtx implements NamespaceContext {
            private Map<String, String> namespaces = new HashMap<String, String>();
            public String getNamespaceURI(String prefix) {
                return namespaces.get(prefix);
            }
            public void addNamespace(String prefix, String namespace) {
                this.namespaces.put(prefix, namespace);
            }
            public void setNamespace(Map<String, String> namespaces) {
                this.namespaces = namespaces;
            }
            // This method isn't necessary for XPath processing.
            public String getPrefix(String uri) {
                throw new UnsupportedOperationException();
            }
            @Override
            public Iterator getPrefixes(String s) {
                throw new UnsupportedOperationException();
            }
    }

}
