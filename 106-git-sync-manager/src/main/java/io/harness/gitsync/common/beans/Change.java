package io.harness.gitsync.common.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class Change {
  private String filePath;
  private String fileContent;
  private String rootPath;
  private String accountId;
  private ChangeType changeType;
  private String oldFilePath;
  @JsonIgnore @SchemaIgnore private boolean syncFromGit;

  public enum ChangeType { ADD, MODIFY, RENAME, DELETE }
}
