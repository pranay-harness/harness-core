package io.harness.gitsync.common.dtos;

import io.harness.data.validator.Trimmed;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitSyncConfigDTO {
  private String identifier;
  @Trimmed private String projectId;
  @Trimmed private String organizationId;
  @Trimmed @NotEmpty private String accountId;
  @Trimmed @NotEmpty private String gitConnectorId;
  @Trimmed @NotEmpty private String repo;
  @Trimmed @NotEmpty private String branch;
  private List<GitSyncFolderConfigDTO> gitSyncFolderConfigDTOs;
}
