package io.harness.gitsync.common.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.ng.core.gitsync.ChangeType;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "GitFileChangeKeys")
@Document("gitFileChange")
@TypeAlias("io.harness.gitsync.common.beans.gitFileChange")
@Entity(value = "yamlChangeSet", noClassnameStored = true)
public class GitFileChange {
  private String filePath;
  private String fileContent;
  private String rootPath;
  private String rootPathId;
  private String accountId;
  private ChangeType changeType;
  private String oldFilePath;
  @JsonIgnore @SchemaIgnore private boolean syncFromGit;
  private String commitId;
  private String objectId;
  private String processingCommitId;
  private boolean changeFromAnotherCommit;
  private Long commitTimeMs;
  private Long processingCommitTimeMs;
  private String commitMessage;
  private String processingCommitMessage;
  private transient YamlGitConfigDTO yamlGitConfig;
}
