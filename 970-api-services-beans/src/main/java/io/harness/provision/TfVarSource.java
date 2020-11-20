package io.harness.provision;

public interface TfVarSource {
  TfVarSourceType getTfVarSourceType();

  enum TfVarSourceType { GIT, SCRIPT_REPOSITORY }
}
