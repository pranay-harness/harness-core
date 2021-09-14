/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

@Slf4j
public class ThreadRule implements TestRule {
  @Override
  public Statement apply(Statement statement, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        Set<String> startThreads = collectThreads();
        statement.evaluate();
        Set<String> endThreads = collectThreads();

        int max_allowed_thread_leaks =
            Integer.parseInt(System.getenv().getOrDefault("MAX_ALLOWED_THREAD_LEAKS", "1000"));

        int diff = endThreads.size() - startThreads.size();
        if (diff > max_allowed_thread_leaks) {
          assertThat(startThreads).isEqualTo(endThreads);
        }
      }

      private Set<String> collectThreads() {
        return Thread.getAllStackTraces()
            .keySet()
            .stream()
            .map(thread -> thread.getName())
            .filter(thread -> !"main".equals(thread))
            .filter(thread -> !"Finalizer".equals(thread))
            .collect(Collectors.toSet());
      }
    };
  }
}
