/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.data.algorithm;

import io.harness.data.structure.UUIDGenerator;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;

public class HashGenerator {
  private HashGenerator() {}
  public static int generateIntegerHash() {
    return Hashing.sha1().hashString(UUIDGenerator.generateUuid(), StandardCharsets.UTF_8).asInt();
  }
}
