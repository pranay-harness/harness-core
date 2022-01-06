/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
