/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.watcher.service;

/**
 * Created by brett on 10/26/17
 */
public interface WatcherService {
  void run(boolean upgrade);
}
