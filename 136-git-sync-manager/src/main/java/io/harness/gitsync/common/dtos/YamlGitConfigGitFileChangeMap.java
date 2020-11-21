package io.harness.gitsync.common.dtos;

import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.GitFileChange;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YamlGitConfigGitFileChangeMap {
  YamlGitConfigDTO yamlGitConfigDTO;
  List<GitFileChange> gitFileChanges;
}
