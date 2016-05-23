package software.wings.common;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * A common universal unique ID generator that will be used throughout the wings application.
 *
 * @author Rishi
 */
public class UUIDGenerator {
  public static String getUuid() {
    return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
  }

  public static void main(String[] args) throws DecoderException {
    String uuid = getUuid();
    System.out.println(uuid + " " + uuid.length());
    byte[] decodedHex = Hex.decodeHex(uuid.toCharArray());
    System.out.println(decodedHex.length);
    byte[] encodedHexB64 = Base64.encodeBase64(decodedHex, false, true);
    System.out.println(decodedHex.length);
    System.out.println(new String(encodedHexB64));
  }
}
