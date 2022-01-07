/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Created by rsingh on 5/23/17.
 */
@Slf4j
public class ThreadDumpRule extends TestWatcher {
  @Override
  protected void failed(Throwable e, Description description) {
    log.info(description.getDisplayName() + " failed. taking thread dumps");

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
            log.info(line);
          }
        }
      } finally {
        pr.destroy();
      }

    } catch (Exception ex) {
      log.error("", ex);
    }
    super.failed(e, description);
  }
}
