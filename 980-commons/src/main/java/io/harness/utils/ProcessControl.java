package io.harness.utils;

import static io.harness.govern.IgnoreThrowable.ignoredOnPurpose;
import static io.harness.threading.Morpheus.sleep;

import io.harness.exception.GeneralException;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.exec.ProcessExecutor;

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

  public static void ensureKilled(final String pid, final Duration timeout) {
    if (StringUtils.isNotBlank(pid)) {
      final long timeoutMs = timeout.toMillis();
      try {
        int tries = 0;
        final int totalTries = 10;
        while (tries < 10 && isProcessRunning(pid)) {
          final String killCommand = "kill " + pid;
          new ProcessExecutor().command("/bin/bash", "-c", killCommand).execute();
          sleep(Duration.ofMillis(timeoutMs / totalTries));
          tries += 1;
        }
        final String forceKillCommand = "kill -9 " + pid;
        new ProcessExecutor().command("/bin/bash", "-c", forceKillCommand).execute();
      } catch (IOException | TimeoutException e) {
        throw new GeneralException("Error killing process " + pid, e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static boolean isProcessRunning(final String pid) throws IOException, InterruptedException, TimeoutException {
    final String isRunningCommand = "kill -0 " + pid;
    return new ProcessExecutor().command("/bin/bash", "-c", isRunningCommand).execute().getExitValue() == 0;
  }
}
