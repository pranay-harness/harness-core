package software.wings.beans.yaml;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Created by anubhaw on 10/16/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class GitFileChange extends Change {
  private String commitId;
  private String objectId;

  public static final class Builder {
    private String commitId;
    private String objectId;
    private String filePath;
    private String fileContent;
    private String accountId;
    private ChangeType changeType;
    private String oldFilePath;

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

    public Builder but() {
      return aGitFileChange()
          .withCommitId(commitId)
          .withObjectId(objectId)
          .withFilePath(filePath)
          .withFileContent(fileContent)
          .withAccountId(accountId)
          .withOldFilePath(oldFilePath)
          .withChangeType(changeType);
    }

    public GitFileChange build() {
      GitFileChange gitFileChange = new GitFileChange();
      gitFileChange.setCommitId(commitId);
      gitFileChange.setObjectId(objectId);
      gitFileChange.setFilePath(filePath);
      gitFileChange.setFileContent(fileContent);
      gitFileChange.setAccountId(accountId);
      gitFileChange.setChangeType(changeType);
      gitFileChange.setOldFilePath(oldFilePath);
      return gitFileChange;
    }
  }
}
