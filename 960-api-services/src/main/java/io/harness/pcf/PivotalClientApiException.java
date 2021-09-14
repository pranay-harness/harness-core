/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pcf;

public class PivotalClientApiException extends Exception {
  public PivotalClientApiException(String s) {
    super(s);
  }

  public PivotalClientApiException(String s, Throwable t) {
    super(s, t);
  }
}
