package io.harness.utils;

import static org.apache.commons.codec.binary.Base64.decodeBase64;

import lombok.extern.slf4j.Slf4j;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.UUID;

@Slf4j
public final class UuidUtils {
  private UuidUtils() {}
  public static String base64StrToUuid(String str) throws BufferUnderflowException, BufferOverflowException {
    byte[] bytes = decodeBase64(str);
    ByteBuffer bb = ByteBuffer.wrap(bytes);
    UUID uuid = new UUID(bb.getLong(), bb.getLong());
    return uuid.toString();
  }

  public static boolean isValidUuidStr(String value) {
    try {
      UUID.fromString(value);
      return true;
    } catch (Exception e) {
      logger.info("{} is not a valid UUID", value);
    }
    return false;
  }
}
