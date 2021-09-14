/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.observer;

import java.util.concurrent.ExecutorService;

public interface AsyncInformObserver extends Observer {
  ExecutorService getInformExecutorService();
}
