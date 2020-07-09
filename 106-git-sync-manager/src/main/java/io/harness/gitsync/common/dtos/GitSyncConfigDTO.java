package io.harness.gitsync.common.dtos;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.Trimmed;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitSyncConfigDTO {
  @NotEmpty @EntityIdentifier private String identifier;
  @Trimmed @NotEmpty private String projectId;
  @Trimmed @NotEmpty private String organizationId;
  @Trimmed @NotEmpty private String accountId;
  @Trimmed @NotEmpty private String gitConnectorId;
  @Trimmed @NotEmpty private String repo;
  @Trimmed @NotEmpty private String branch;
  private List<GitSyncFolderConfigDTO> gitSyncFolderConfigDTOs;
}
