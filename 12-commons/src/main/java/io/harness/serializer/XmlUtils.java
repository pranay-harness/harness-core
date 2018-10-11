/**
 *
 */

package io.harness.serializer;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * The Class XmlUtils.
 *
 * @author Rishi
 */
public class XmlUtils {
  private static final Logger logger = LoggerFactory.getLogger(XmlUtils.class);

  /**
   * Xpath.
   *
   * @param content    the content
   * @param expression the expression
   * @return the string
   * @throws ParserConfigurationException the parser configuration exception
   * @throws SAXException                 the SAX exception
   * @throws IOException                  Signals that an I/O exception has occurred.
   * @throws XPathExpressionException     the x path expression exception
   */
  public static String xpath(String content, String expression)
      throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
    logger.debug("xpath request - expression: {}, content: {}", expression, content);
    Document document = parse(content);
    return xpath(document, expression);
  }

  /**
   * Xpath.
   *
   * @param document   the document
   * @param expression the expression
   * @return the string
   * @throws XPathExpressionException the x path expression exception
   */
  public static String xpath(Document document, String expression) throws XPathExpressionException {
    XPath xpath = XPathFactory.newInstance().newXPath();
    String retValue = xpath.evaluate(expression, document);
    logger.debug("xpath request - return value: {}", retValue);
    return retValue;
  }

  /**
   * Parses the.
   *
   * @param content the content
   * @return the document
   * @throws ParserConfigurationException the parser configuration exception
   * @throws SAXException                 the SAX exception
   * @throws IOException                  Signals that an I/O exception has occurred.
   */
  public static Document parse(String content) throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(new ByteArrayInputStream(content.getBytes(UTF_8)));
  }
}
