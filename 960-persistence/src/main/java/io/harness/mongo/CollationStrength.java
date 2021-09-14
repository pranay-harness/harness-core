/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo;

public enum CollationStrength {
  PRIMARY(1),
  SECONDARY(2),
  TERTIARY(3),
  QUATERNARY(4),
  IDENTICAL(5);

  private final int code;

  public int getCode() {
    return this.code;
  }

  CollationStrength(int code) {
    this.code = code;
  }
}
