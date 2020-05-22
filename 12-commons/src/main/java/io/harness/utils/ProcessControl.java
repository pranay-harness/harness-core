package io.harness.utils;

import static io.harness.govern.IgnoreThrowable.ignoredOnPurpose;
import static io.harness.threading.Morpheus.sleep;

import com.google.common.base.Splitter;

import io.harness.exception.GeneralException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@UtilityClass
@Slf4j
public class ProcessControl {
  public static int myProcessId() {
    String pid = Splitter.on("@").split(ManagementFactory.getRuntimeMXBean().getName()).iterator().next();
    try {
      return Integer.parseInt(pid);
    } catch (Exception ex) {
      ignoredOnPurpose(ex);
      return -1;
    }
  }

  public static void ensureKilled(String pid, Duration timeout) {
    long timeoutMs = timeout.toMillis();
    try {
      int tries = 0;
      int totalTries = 10;
      boolean isRunning = new ProcessExecutor().command("kill", "-0", pid).execute().getExitValue() == 0;
      while (tries < 10 && isRunning) {
        new ProcessExecutor().command("kill", pid).execute();
        sleep(Duration.ofMillis(timeoutMs / totalTries));
        tries += 1;
        isRunning = new ProcessExecutor().command("kill", "-0", pid).execute().getExitValue() == 0;
      }
      new ProcessExecutor().command("kill", "-9", pid).execute();
    } catch (IOException | TimeoutException e) {
      throw new GeneralException("Error killing process " + pid, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
