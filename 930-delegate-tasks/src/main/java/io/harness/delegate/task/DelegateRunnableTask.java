/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task;

import io.harness.delegate.beans.DelegateResponseData;

import java.io.IOException;

public interface DelegateRunnableTask extends Runnable {
  @Deprecated DelegateResponseData run(Object[] parameters);
  DelegateResponseData run(TaskParameters parameters) throws IOException;
}
