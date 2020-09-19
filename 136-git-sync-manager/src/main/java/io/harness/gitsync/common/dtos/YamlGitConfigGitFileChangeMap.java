package io.harness.gitsync.common.dtos;

import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.GitFileChange;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class YamlGitConfigGitFileChangeMap {
  YamlGitConfigDTO yamlGitConfigDTO;
  List<GitFileChange> gitFileChanges;
}
