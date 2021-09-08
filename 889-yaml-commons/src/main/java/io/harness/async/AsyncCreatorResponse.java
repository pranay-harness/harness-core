package io.harness.async;

import io.harness.pms.contracts.plan.Dependencies;

import java.util.List;

public interface AsyncCreatorResponse {
  Dependencies getDependencies();

  List<String> getErrorMessages();
}
