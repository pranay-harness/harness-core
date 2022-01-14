package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.GitFileChange;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(DX)
public class YamlGitConfigGitFileChangeMap {
  YamlGitConfigDTO yamlGitConfigDTO;
  List<GitFileChange> gitFileChanges;
}
