/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
