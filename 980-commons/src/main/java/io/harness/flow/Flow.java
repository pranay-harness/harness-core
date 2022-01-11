/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
