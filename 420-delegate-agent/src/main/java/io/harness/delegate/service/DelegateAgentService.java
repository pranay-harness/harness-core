/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.service;

public interface DelegateAgentService {
  void run(boolean watched);

  void pause();

  void stop();

  void freeze();

  boolean areAllClientToolsInstalled();
}
