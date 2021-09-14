/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.client.impl.tailer;

import lombok.experimental.UtilityClass;
import net.openhft.chronicle.queue.ExcerptTailer;

@UtilityClass
class QueueUtils {
  static void moveToIndex(ExcerptTailer tailer, long index) {
    if (index == 0) {
      tailer.toStart();
    } else {
      tailer.moveToIndex(index);
    }
  }
}
