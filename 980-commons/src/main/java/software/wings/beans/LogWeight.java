/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

public enum LogWeight {
  Normal(0),
  Bold(1);

  final int value;

  LogWeight(int value) {
    this.value = value;
  }
}
