/**
 *
 */
package software.wings.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

/**
 * @author Rishi
 */
public class XmlUtilsTest {
  @Test
  public void shouldGetXpath()
      throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
    String content = "<widgets><widget><manufacturer>abc</manufacturer><dimensions/></widget></widgets>";
    String expression = "//widget/manufacturer/text()";
    String text = XmlUtils.xpath(content, expression);

    assertThat(text).isNotNull();
    assertThat(text).isEqualTo("abc");
  }
}
