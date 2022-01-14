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
