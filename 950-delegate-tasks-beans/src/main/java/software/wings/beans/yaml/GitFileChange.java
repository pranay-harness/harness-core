/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.yaml;

import io.harness.git.model.ChangeType;

import software.wings.yaml.gitSync.YamlGitConfig;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Created by anubhaw on 10/16/17.
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
public class GitFileChange extends Change {
  private String commitId;
  private String objectId;
  private transient YamlGitConfig yamlGitConfig;
  private String processingCommitId;
  private boolean changeFromAnotherCommit;
  private Long commitTimeMs;
  private Long processingCommitTimeMs;
  private String commitMessage;
  private String processingCommitMessage;
  private String entityId;

  public static final class Builder {
    private String commitId;
    private String objectId;
    private String filePath;
    private String fileContent;
    private String accountId;
    private ChangeType changeType;
    private String oldFilePath;
    @JsonIgnore @SchemaIgnore private boolean syncFromGit;
    private transient YamlGitConfig yamlGitConfig;
    private String processingCommitId;
    private boolean changeFromAnotherCommit;
    private Long commitTimeMs;
    private Long processingCommitTimeMs;
    private String commitMessage;
    private String processingCommitMessage;
    private String entityId;

    private Builder() {}

    public static Builder aGitFileChange() {
      return new Builder();
    }

    public Builder withCommitId(String commitId) {
      this.commitId = commitId;
      return this;
    }

    public Builder withObjectId(String objectId) {
      this.objectId = objectId;
      return this;
    }

    public Builder withFilePath(String filePath) {
      this.filePath = filePath;
      return this;
    }

    public Builder withFileContent(String fileContent) {
      this.fileContent = fileContent;
      return this;
    }

    public Builder withOldFilePath(String oldFilePath) {
      this.oldFilePath = oldFilePath;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withChangeType(ChangeType changeType) {
      this.changeType = changeType;
      return this;
    }

    public Builder withSyncFromGit(boolean syncFromGit) {
      this.syncFromGit = syncFromGit;
      return this;
    }

    public Builder withYamlGitConfig(YamlGitConfig yamlGitConfig) {
      this.yamlGitConfig = yamlGitConfig;
      return this;
    }

    public Builder withChangeFromAnotherCommit(boolean changeFromAnotherCommit) {
      this.changeFromAnotherCommit = changeFromAnotherCommit;
      return this;
    }

    public Builder withProcessingCommitId(String processingCommitId) {
      this.processingCommitId = processingCommitId;
      return this;
    }
    public Builder withCommitTimeMs(Long commitTimeMs) {
      this.commitTimeMs = commitTimeMs;
      return this;
    }

    public Builder withProcessingCommitTimeMs(Long processingCommitTimeMs) {
      this.processingCommitTimeMs = processingCommitTimeMs;
      return this;
    }

    public Builder withCommitMessage(String commitMessage) {
      this.commitMessage = commitMessage;
      return this;
    }

    public Builder withProcessingCommitMessage(String processingCommitMessage) {
      this.processingCommitMessage = processingCommitMessage;
      return this;
    }

    public Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    public Builder but() {
      return aGitFileChange()
          .withCommitId(commitId)
          .withProcessingCommitId(processingCommitId)
          .withObjectId(objectId)
          .withFilePath(filePath)
          .withFileContent(fileContent)
          .withAccountId(accountId)
          .withOldFilePath(oldFilePath)
          .withChangeType(changeType)
          .withSyncFromGit(syncFromGit)
          .withYamlGitConfig(yamlGitConfig)
          .withChangeFromAnotherCommit(changeFromAnotherCommit)
          .withCommitTimeMs(commitTimeMs)
          .withProcessingCommitTimeMs(processingCommitTimeMs)
          .withCommitMessage(commitMessage)
          .withProcessingCommitMessage(processingCommitMessage)
          .withEntityId(entityId);
    }

    public GitFileChange build() {
      GitFileChange gitFileChange = new GitFileChange();
      gitFileChange.setCommitId(commitId);
      gitFileChange.setProcessingCommitId(processingCommitId);
      gitFileChange.setObjectId(objectId);
      gitFileChange.setFilePath(filePath);
      gitFileChange.setFileContent(fileContent);
      gitFileChange.setAccountId(accountId);
      gitFileChange.setChangeType(changeType);
      gitFileChange.setOldFilePath(oldFilePath);
      gitFileChange.setSyncFromGit(syncFromGit);
      gitFileChange.setYamlGitConfig(yamlGitConfig);
      gitFileChange.setChangeFromAnotherCommit(changeFromAnotherCommit);
      gitFileChange.setCommitTimeMs(commitTimeMs);
      gitFileChange.setProcessingCommitTimeMs(processingCommitTimeMs);
      gitFileChange.setCommitMessage(commitMessage);
      gitFileChange.setProcessingCommitMessage(processingCommitMessage);
      gitFileChange.setEntityId(entityId);
      return gitFileChange;
    }
  }
}
