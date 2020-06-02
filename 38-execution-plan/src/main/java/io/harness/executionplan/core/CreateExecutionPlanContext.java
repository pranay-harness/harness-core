package io.harness.executionplan.core;

import java.util.Optional;

/**
 * Plan creators to use this for co-ordination and data sharing.
 * Attributes can be created and shared across plan creators
 */
public interface CreateExecutionPlanContext {
  String getAccountId();
  <T> Optional<T> getAttribute(String key);
  <T> void addAttribute(String key, T value);
}
