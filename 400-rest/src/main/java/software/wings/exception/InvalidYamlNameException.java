/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.exception;

public class InvalidYamlNameException extends RuntimeException {
  public InvalidYamlNameException(String message, Throwable cause) {
    super(message, cause);
  }
  public InvalidYamlNameException(String message) {
    super(message);
  }
}
