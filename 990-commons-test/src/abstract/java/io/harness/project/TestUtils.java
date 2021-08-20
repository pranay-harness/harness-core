package io.harness.project;

public class TestUtils {
  public static boolean isTestProcess() {
    for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
      if (element.getClassName().startsWith("io.harness.rule.")) {
        return true;
      }
    }
    return false;
  }
}
