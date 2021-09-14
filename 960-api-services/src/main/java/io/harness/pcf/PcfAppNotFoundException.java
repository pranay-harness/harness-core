/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pcf;

public class PcfAppNotFoundException extends PivotalClientApiException {
  public PcfAppNotFoundException(String s) {
    super(s);
  }

  public PcfAppNotFoundException(String s, Throwable t) {
    super(s, t);
  }
}
