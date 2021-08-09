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
