/**
 *
 */

package software.wings.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * The Class XmlUtilsTest.
 *
 * @author Rishi
 */
public class KryoUtilsTest {
  @Test
  public void shouldGetXpath() {
    String test = "Neque porro quisquam est qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit...";
    final byte[] deflatedBytes = KryoUtils.asDeflatedBytes(test);
    String inflatedObject = (String) KryoUtils.asInflatedObject(deflatedBytes);

    assertThat(test).isEqualTo(inflatedObject);
  }
}
