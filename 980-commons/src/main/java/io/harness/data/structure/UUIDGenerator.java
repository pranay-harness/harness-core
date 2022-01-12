/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.data.structure;

import static com.fasterxml.uuid.Generators.timeBasedGenerator;
import static org.apache.commons.codec.binary.Base64.decodeBase64;

import java.nio.ByteBuffer;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;

/**
 * A universal unique ID generator.
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
    return convertToBase64String(uuid);
  }

  public static String generateTimeBasedUuid() {
    UUID uuid = timeBasedGenerator().generate();
    return convertToBase64String(uuid);
  }

  protected static String convertToBase64String(UUID uuid) {
    byte[] bytes = new byte[16];
    ByteBuffer uuidBytes = ByteBuffer.wrap(bytes);
    uuidBytes.putLong(uuid.getMostSignificantBits());
    uuidBytes.putLong(uuid.getLeastSignificantBits());
    return Base64.encodeBase64URLSafeString(bytes);
  }

  public static UUID convertFromBase64(String uuid) {
    byte[] bytes = decodeBase64(uuid);
    ByteBuffer bb = ByteBuffer.wrap(bytes);
    return new UUID(bb.getLong(), bb.getLong());
  }

  /**
   * Converts Base64 uuid to canonical form, xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.
   *
   * @return the uuid
   */
  public static String convertBase64UuidToCanonicalForm(String base64Uuid) {
    return UUID.nameUUIDFromBytes(decodeBase64(base64Uuid)).toString();
  }
}
