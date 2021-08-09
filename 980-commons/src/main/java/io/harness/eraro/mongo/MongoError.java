package io.harness.eraro.mongo;

import lombok.Getter;

public enum MongoError {
  DUPLICATE_KEY(11000);

  @Getter private int errorCode;

  MongoError(int errorCode) {
    this.errorCode = errorCode;
  }
}
