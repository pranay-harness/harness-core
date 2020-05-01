package software.wings.delegatetasks.citasks.cik8handler;

/**
 * Listener that processes watch channel events of K8 command execution including opening, closure and failure events.
 */

import io.fabric8.kubernetes.client.dsl.ExecWatch;
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
    logger.info("Channel opened: {}", response);
  }

  @Override
  public void onFailure(Throwable t, Response response) {
    logger.info("Failed to execute: {} {}", response, t);
    cmdStatus.set(false);
    execWait.release();
  }

  @Override
  public void onClose(int code, String reason) {
    logger.info("Channel closed with code: {}, reason: {}", code, reason);
    cmdStatus.set(true);
    execWait.release();
  }

  /**
   * Waits for the command to execute and return the executed command's status code.
   */
  public boolean getReturnStatus(ExecWatch watch, Integer timeoutSecs) throws InterruptedException, TimeoutException {
    if (!execWait.tryAcquire((long) timeoutSecs, TimeUnit.SECONDS)) {
      watch.close();
      throw new TimeoutException("Failed to wait for command to finish");
    }

    return cmdStatus.get();
  }
}