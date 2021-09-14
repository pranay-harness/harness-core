/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.eraro.mongo;

import lombok.Getter;

public enum MongoError {
  DUPLICATE_KEY(11000);

  @Getter private int errorCode;

  MongoError(int errorCode) {
    this.errorCode = errorCode;
  }
}
