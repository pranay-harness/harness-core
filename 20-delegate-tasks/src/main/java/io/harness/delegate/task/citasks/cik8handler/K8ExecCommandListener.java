package io.harness.delegate.task.citasks.cik8handler;

/**
 * Listener that processes watch channel events of K8 command execution including opening, closure and failure events.
 */

import static java.lang.String.format;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class K8ExecCommandListener implements ExecCommandListener {
  private Semaphore execWait;
  private AtomicBoolean cmdStatus;

  public K8ExecCommandListener() {
    this.execWait = new Semaphore(0);
    this.cmdStatus = new AtomicBoolean(false);
  }

  @Override
  public void onOpen(Response response) {
    log.info("Channel opened: {}", response);
  }

  @Override
  public void onFailure(Throwable t, Response response) {
    log.info("Failed to execute: {} {}", response, t);
    cmdStatus.set(false);
    execWait.release();
  }

  @Override
  public void onClose(int code, String reason) {
    log.info("Channel closed with code: {}, reason: {}", code, reason);
    cmdStatus.set(true);
    execWait.release();
  }

  /**
   * Waits for the command to execute and returns whether the command is executed or not.
   */
  public boolean isCommandExecutionComplete(Integer timeoutSecs) throws InterruptedException, TimeoutException {
    if (!execWait.tryAcquire((long) timeoutSecs, TimeUnit.SECONDS)) {
      throw new TimeoutException(format("Command execution failed to complete in  %d", timeoutSecs));
    }

    return cmdStatus.get();
  }
}
