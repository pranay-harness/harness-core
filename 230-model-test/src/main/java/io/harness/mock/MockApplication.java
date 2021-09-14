/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mock;

import io.harness.mock.server.MockServer;

public class MockApplication {
  public static void main(String[] args) {
    new MockServer().start();
  }
}
