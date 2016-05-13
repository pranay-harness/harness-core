/**
 *
 */
package software.wings.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.inject.Singleton;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * @author Rishi
 */
@Singleton
public class XmlUtils {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public String xpath(String content, String expression)
      throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
    logger.debug("xpath request - expression: {}, content: {}", expression, content);
    Document document = parse(content);
    return xpath(document, expression);
  }

  public String xpath(Document document, String expression) throws XPathExpressionException {
    XPath xpath = XPathFactory.newInstance().newXPath();
    String retValue = xpath.evaluate(expression, document);
    logger.debug("xpath request - return value: {}", retValue);
    return retValue;
  }

  public Document parse(String content) throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document document = builder.parse(new ByteArrayInputStream(content.getBytes()));
    return document;
  }
}
