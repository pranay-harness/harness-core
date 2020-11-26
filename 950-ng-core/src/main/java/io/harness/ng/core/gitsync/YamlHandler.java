package io.harness.ng.core.gitsync;

import io.harness.EntityType;
import io.harness.git.model.GitFileChange;

public interface YamlHandler {
  boolean canProcess(GitFileChange gitFileChange, EntityType entityType);

  boolean validate(GitFileChange gitFileChange, String projectIdentifier, String orgIdentifier, String accountId);

  String handleAddition(GitFileChange gitFileChange, String projectIdentifier, String orgIdentifier, String accountId);

  String handleDeletion(GitFileChange gitFileChange, String projectIdentifier, String orgIdentifier, String accountId);

  String handleModify(GitFileChange gitFileChange, String projectIdentifier, String orgIdentifier, String accountId);
}
