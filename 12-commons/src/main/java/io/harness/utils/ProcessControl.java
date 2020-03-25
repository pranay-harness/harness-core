package io.harness.utils;

import static io.harness.threading.Morpheus.sleep;

import io.harness.exception.GeneralException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@UtilityClass
@Slf4j
public class ProcessControl {
  public void ensureKilled(String pid, Duration timeout) {
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
      logger.error("Error killing process {}", pid, e);
      throw new GeneralException("Error killing process " + pid, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
