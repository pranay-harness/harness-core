package io.harness.logging;

import ch.qos.logback.core.rolling.TriggeringPolicyBase;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class LoggingTriggerPolicy<E> extends TriggeringPolicyBase<E> {
  static AtomicBoolean doRolling = new AtomicBoolean();

  @Override
  public boolean isTriggeringEvent(File file, E e) {
    return doRolling.getAndSet(false);
  }

  public static void resetRollingPolicy() {
    log.info("reset rolling policy");
    doRolling.set(true);
  }
}
