/**
 *
 */

package io.harness.serializer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

/**
 * The Class XmlUtilsTest.
 *
 * @author Rishi
 */
public class XmlUtilsTest {
  /**
   * Should get xpath.
   *
   * @throws XPathExpressionException     the x path expression exception
   * @throws ParserConfigurationException the parser configuration exception
   * @throws SAXException                 the SAX exception
   * @throws IOException                  Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldGetXpath()
      throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
    String content = "<widgets><widget><manufacturer>abc</manufacturer><dimensions/></widget></widgets>";
    String expression = "//widget/manufacturer/text()";
    String text = XmlUtils.xpath(content, expression);

    assertThat(text).isEqualTo("abc");
  }
}
