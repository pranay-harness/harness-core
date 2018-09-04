package software.wings.beans.yaml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.ToString;

/**
 * @author rktummala on 10/16/17
 */
@Data
@ToString(exclude = "fileContent")
public class Change {
  private String filePath;
  private String fileContent;
  private String accountId;
  private ChangeType changeType;
  private String oldFilePath;
  @JsonIgnore @SchemaIgnore private boolean syncFromGit;

  public enum ChangeType { ADD, MODIFY, RENAME, DELETE }

  @SuppressFBWarnings("CN_IMPLEMENTS_CLONE_BUT_NOT_CLONEABLE")
  public Builder clone() {
    return Builder.aFileChange()
        .withFilePath(getFilePath())
        .withFileContent(getFileContent())
        .withAccountId(getAccountId())
        .withChangeType(getChangeType())
        .withOldFilePath(getOldFilePath())
        .withSyncFromGit(isSyncFromGit());
  }

  public static final class Builder {
    private Change change;

    private Builder() {
      change = new Change();
    }

    public static Builder aFileChange() {
      return new Builder();
    }

    public Builder withFilePath(String filePath) {
      change.setFilePath(filePath);
      return this;
    }

    public Builder withFileContent(String fileContent) {
      change.setFileContent(fileContent);
      return this;
    }

    public Builder withAccountId(String accountId) {
      change.setAccountId(accountId);
      return this;
    }

    public Builder withChangeType(ChangeType changeType) {
      change.setChangeType(changeType);
      return this;
    }

    public Builder withOldFilePath(String oldFilePath) {
      change.setOldFilePath(oldFilePath);
      return this;
    }

    public Builder withSyncFromGit(boolean syncFromGit) {
      change.setSyncFromGit(syncFromGit);
      return this;
    }

    public Builder but() {
      return aFileChange()
          .withFilePath(change.getFilePath())
          .withFileContent(change.getFileContent())
          .withAccountId(change.getAccountId())
          .withChangeType(change.getChangeType())
          .withOldFilePath(change.getOldFilePath())
          .withSyncFromGit(change.isSyncFromGit());
    }

    public Change build() {
      return change;
    }
  }
}
