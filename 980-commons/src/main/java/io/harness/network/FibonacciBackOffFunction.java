/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.network;

import java.io.IOException;

@FunctionalInterface
public interface FibonacciBackOffFunction<T> {
  T execute() throws IOException;
}
