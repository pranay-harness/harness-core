package software.wings.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by rsingh on 5/23/17.
 */
@Slf4j
public class ThreadDumpRule extends TestWatcher {
  @Override
  protected void failed(Throwable e, Description description) {
    logger.info(description.getDisplayName() + " failed. taking thread dumps");

    try {
      String cmd = "killall -3 java";
      Runtime run = Runtime.getRuntime();
      Process pr = run.exec(cmd);
      try {
        pr.waitFor();
        try (InputStreamReader reader = new InputStreamReader(pr.getInputStream())) {
          BufferedReader buf = new BufferedReader(reader);
          String line = "";
          while ((line = buf.readLine()) != null) {
            logger.info(line);
          }
        }
      } finally {
        pr.destroy();
      }

    } catch (Exception ex) {
      logger.error("", ex);
    }
    super.failed(e, description);
  }
}
