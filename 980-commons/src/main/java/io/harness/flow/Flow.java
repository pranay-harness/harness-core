/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.flow;

import static io.harness.threading.Morpheus.sleep;

import java.time.Duration;

public class Flow {
  public interface Repeatable {
    void run() throws Exception;
  }

  public static void retry(int tries, Duration interval, Repeatable repeatable) throws Exception {
    for (int i = 1; i < tries; ++i) {
      try {
        repeatable.run();
        return;
      } catch (Exception ignore) {
        // do nothing
      }
      sleep(interval);
    }
    repeatable.run();
  }
}
