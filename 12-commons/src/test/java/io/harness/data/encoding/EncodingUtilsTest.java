package io.harness.data.encoding;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.decodeBase64ToString;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import org.junit.Test;

import java.net.URL;

public class EncodingUtilsTest {
  @Test
  public void testCompression() throws Exception {
    StringBuilder stringToCompress = new StringBuilder(generateUuid());
    for (int i = 0; i < 100; i++) {
      stringToCompress.append(' ').append(generateUuid());
    }

    byte[] compressedString = compressString(stringToCompress.toString());
    String deCompressedString = deCompressString(compressedString);
    assertEquals(stringToCompress.toString(), deCompressedString);
  }

  @Test
  public void testCompressionWithStringEncoding() throws Exception {
    StringBuilder stringToCompress = new StringBuilder(generateUuid());
    for (int i = 0; i < 100; i++) {
      stringToCompress.append(' ').append(generateUuid());
    }

    String compressedString = encodeBase64(compressString(stringToCompress.toString()));
    String deCompressedString = deCompressString(decodeBase64(compressedString));
    assertEquals(stringToCompress.toString(), deCompressedString);
  }

  @Test
  public void testBase64() throws Exception {
    URL url = this.getClass().getResource("/dos-config.yaml");
    String fileContents = Resources.toString(url, Charsets.UTF_8);
    assertEquals(decodeBase64ToString(encodeBase64(fileContents)), fileContents);
  }
}
