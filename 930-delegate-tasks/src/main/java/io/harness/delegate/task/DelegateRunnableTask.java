package io.harness.delegate.task;

import io.harness.delegate.beans.DelegateResponseData;
import org.jose4j.lang.JoseException;

import java.io.IOException;

public interface DelegateRunnableTask extends Runnable {
  @Deprecated DelegateResponseData run(Object[] parameters);
  DelegateResponseData run(TaskParameters parameters) throws IOException, JoseException;
}
