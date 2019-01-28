package io.harness.expression;

import io.harness.serializer.XmlUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

public class XmlFunctor implements ExpressionFunctor {
  public Object select(String xpath, String xml)
      throws ParserConfigurationException, SAXException, XPathExpressionException, IOException {
    return XmlUtils.xpath(xml, xpath);
  }
}
