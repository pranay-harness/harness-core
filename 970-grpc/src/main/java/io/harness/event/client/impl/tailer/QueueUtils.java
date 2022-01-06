/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
