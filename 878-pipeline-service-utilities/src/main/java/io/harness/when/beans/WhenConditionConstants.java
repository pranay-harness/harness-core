package io.harness.when.beans;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public interface WhenConditionConstants {
  String SUCCESS = "Success";
  String FAILURE = "Failure";
  String ALL = "All";
}
