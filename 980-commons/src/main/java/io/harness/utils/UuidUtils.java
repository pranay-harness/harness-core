/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.utils;

import static org.apache.commons.codec.binary.Base64.decodeBase64;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

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
      log.info("{} is not a valid UUID", value);
    }
    return false;
  }
}
