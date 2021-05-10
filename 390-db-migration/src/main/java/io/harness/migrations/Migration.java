package io.harness.migrations;

import io.harness.annotations.dev.HarnessModule;
public interface Migration {
  void migrate();
}
