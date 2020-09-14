package io.harness.mock;

import io.harness.mock.server.MockServer;

public class MockApplication {
  public static void main(String[] args) {
    new MockServer().start();
  }
}
