package software.wings.delegatetasks.citasks.cik8handler;

import io.fabric8.kubernetes.client.dsl.ExecListener;

import java.util.concurrent.TimeoutException;

public interface ExecCommandListener extends ExecListener {
  boolean isCommandExecutionComplete(Integer timeoutSecs) throws InterruptedException, TimeoutException;
}