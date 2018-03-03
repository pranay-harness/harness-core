package software.wings.integration;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by rsingh on 5/23/17.
 */
public class ThreadDumpRule extends TestWatcher {
  private static final Logger logger = LoggerFactory.getLogger(ThreadDumpRule.class);

  @Override
  protected void failed(Throwable e, Description description) {
    System.out.println(description.getDisplayName() + " failed. taking thread dumps");

    try {
      String cmd = "killall -3 java";
      Runtime run = Runtime.getRuntime();
      Process pr = run.exec(cmd);
      pr.waitFor();
      BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
      String line = "";
      while ((line = buf.readLine()) != null) {
        System.out.println(line);
      }
    } catch (Exception ex) {
      logger.error("", ex);
    }
    super.failed(e, description);
  }
}
