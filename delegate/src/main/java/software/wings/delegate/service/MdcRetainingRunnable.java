package software.wings.delegate.service;

import org.slf4j.MDC;

import java.util.Map;

/**
 * Created by peeyushaggarwal on 1/27/17.
 */
public class MdcRetainingRunnable implements Runnable {
  private final Runnable runnable;
  private final Map context;

  public MdcRetainingRunnable(Runnable runnable) {
    this.runnable = runnable;
    this.context = MDC.getCopyOfContextMap();
  }

  @Override
  public void run() {
    Map originalContext = MDC.getCopyOfContextMap();
    MDC.setContextMap(context);
    try {
      runnable.run();
    } finally {
      MDC.setContextMap(originalContext);
    }
  }
}
