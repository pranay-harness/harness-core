package io.harness.ng.gitsync.handlers;

import io.harness.EntityType;
import io.harness.git.model.GitFileChange;
import io.harness.ng.core.gitsync.YamlHandler;

public class ConnectorYamlHandler implements YamlHandler {
  @Override
  public boolean canProcess(GitFileChange gitFileChange, EntityType entityType) {
    return false;
  }

  @Override
  public boolean validate(
      GitFileChange gitFileChange, String projectIdentifier, String orgIdentifier, String accountId) {
    return false;
  }

  @Override
  public String handleAddition(
      GitFileChange gitFileChange, String projectIdentifier, String orgIdentifier, String accountId) {
    return null;
  }

  @Override
  public String handleDeletion(
      GitFileChange gitFileChange, String projectIdentifier, String orgIdentifier, String accountId) {
    return null;
  }

  @Override
  public String handleModify(
      GitFileChange gitFileChange, String projectIdentifier, String orgIdentifier, String accountId) {
    return null;
  }
}
