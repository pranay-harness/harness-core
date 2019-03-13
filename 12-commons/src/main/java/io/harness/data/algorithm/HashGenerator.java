package io.harness.data.algorithm;

import com.google.common.hash.Hashing;

import io.harness.data.structure.UUIDGenerator;

import java.nio.charset.StandardCharsets;

public class HashGenerator {
  public static int generateIntegerHash() {
    return Hashing.sha1().hashString(UUIDGenerator.generateUuid(), StandardCharsets.UTF_8).asInt();
  }
}
