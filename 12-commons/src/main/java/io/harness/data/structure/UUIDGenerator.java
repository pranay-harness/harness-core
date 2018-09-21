package io.harness.data.structure;

import org.apache.commons.codec.binary.Base64;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * A universal unique ID generator.
 *
 * @author Rishi
 */
public final class UUIDGenerator {
  private UUIDGenerator() {}

  /**
   * Generates a random uuid in base64 format.
   *
   * @return the uuid
   */
  public static String generateUuid() {
    UUID uuid = UUID.randomUUID();
    byte[] bytes = new byte[16];
    ByteBuffer uuidBytes = ByteBuffer.wrap(bytes);
    uuidBytes.putLong(uuid.getMostSignificantBits());
    uuidBytes.putLong(uuid.getLeastSignificantBits());
    return Base64.encodeBase64URLSafeString(bytes);
  }

  /**
   * Converts Base64 uuid to canonical form, xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.
   *
   * @return the uuid
   */
  public static String convertBase64UuidToCanonicalForm(String base64Uuid) {
    return UUID.nameUUIDFromBytes(Base64.decodeBase64(base64Uuid)).toString();
  }

  /**
   * Graph id generator string.
   *
   * @param prefix the prefix
   * @return the string
   */
  public static String graphIdGenerator(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString();
  }
}
