package io.harness.environment;

import com.google.inject.Singleton;

@Singleton
public class SystemEnvironment {
  public String get(String name) {
    return System.getenv(name);
  }
}
