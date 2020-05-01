package software.wings.delegatetasks.citasks.cik8handler;

import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;

import java.util.concurrent.TimeoutException;

public interface ExecCommandListener extends ExecListener {
  boolean getReturnStatus(ExecWatch watch, Integer timeoutSecs) throws InterruptedException, TimeoutException;
}