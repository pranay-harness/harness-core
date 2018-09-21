package io.harness.data.compression;

import static io.harness.data.compression.CompressionUtils.compressString;
import static io.harness.data.compression.CompressionUtils.deCompressString;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Base64;

/**
 * Created by rsingh on 8/24/18.
 */
public class StringCompressionTest {
  @Test
  public void testCompression() throws Exception {
    String stringToCompress = generateUuid();
    for (int i = 0; i < 100; i++) {
      stringToCompress += " " + generateUuid();
    }

    byte[] compressedString = compressString(stringToCompress);
    String deCompressedString = deCompressString(compressedString);
    assertEquals(stringToCompress, deCompressedString);
  }

  @Test
  public void testCompressionWithStringEncoding() throws Exception {
    String stringToCompress = generateUuid();
    for (int i = 0; i < 100; i++) {
      stringToCompress += " " + generateUuid();
    }

    String compressedString = Base64.getEncoder().encodeToString(compressString(stringToCompress));
    String deCompressedString = deCompressString(Base64.getDecoder().decode(compressedString));
    assertEquals(stringToCompress, deCompressedString);
  }
}
