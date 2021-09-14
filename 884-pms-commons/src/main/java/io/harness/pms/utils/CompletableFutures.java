/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CompletableFutures<T> {
  private final Executor executor;
  private final List<CompletableFuture<T>> completableFutures = new ArrayList<>();

  public CompletableFutures(Executor executor) {
    this.executor = executor;
  }

  public void supplyAsync(Supplier<T> supplier) {
    completableFutures.add(CompletableFuture.supplyAsync(supplier, executor));
  }

  public CompletableFuture<List<T>> allOf() {
    return CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]))
        .thenApply(ignored -> completableFutures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
  }
}
