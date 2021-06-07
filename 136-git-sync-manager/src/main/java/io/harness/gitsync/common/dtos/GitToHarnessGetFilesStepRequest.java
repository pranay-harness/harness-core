package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.common.beans.GitToHarnessProgress;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(DX)
public class GitToHarnessGetFilesStepRequest {
  YamlChangeSetDTO yamlChangeSetDTO;
  List<YamlGitConfigDTO> yamlGitConfigDTOList;
  GitToHarnessProgress gitToHarnessProgress;
}
