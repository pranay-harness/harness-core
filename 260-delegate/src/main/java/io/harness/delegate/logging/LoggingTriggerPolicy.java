package io.harness.delegate.logging;

import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TriggeringPolicyBase;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingTriggerPolicy<E> extends SizeAndTimeBasedFNATP<E> {
  private boolean started = false;

  @Override
  public boolean isTriggeringEvent(File file, E e) {
    if (!started) {
      nextCheck = 0L;
      return started = true;
    }

    return super.isTriggeringEvent(file, e);
  }

}
