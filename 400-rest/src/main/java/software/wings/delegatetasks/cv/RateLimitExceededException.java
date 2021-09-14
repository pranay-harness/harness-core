/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.cv;

@Deprecated
public class RateLimitExceededException extends DataCollectionException {
  public RateLimitExceededException(Exception e) {
    super(e);
  }

  public RateLimitExceededException(String message) {
    super(message);
  }
}
