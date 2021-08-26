package software.wings.beans.yaml;

import static io.harness.annotations.dev.HarnessModule._870_CG_YAML;
import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.git.model.ChangeType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.ToString;

/**
 * @author rktummala on 10/16/17
 */
@Data
@ToString(exclude = "fileContent")
@TargetModule(_870_CG_YAML)
@OwnedBy(DX)
public class Change {
  private String filePath;
  private String fileContent;
  private String accountId;
  private ChangeType changeType;
  private String oldFilePath;
  @JsonIgnore @SchemaIgnore private boolean syncFromGit;

  public Builder toBuilder() {
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
