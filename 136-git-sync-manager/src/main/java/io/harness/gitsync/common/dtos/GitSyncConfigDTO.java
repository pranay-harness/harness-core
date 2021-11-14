package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
import io.harness.delegate.beans.connector.ConnectorType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "GitSyncConfigDTOKeys")
@Schema(name = "GitSyncConfig", description = "Contains information of Git Sync Config")
@OwnedBy(DX)
@ApiModel("GitSyncConfig")
public class GitSyncConfigDTO {
  @Trimmed @NotEmpty private String identifier;
  @Trimmed @NotEmpty private String name;
  @Trimmed private String projectIdentifier;
  @Trimmed private String orgIdentifier;
  @Trimmed @NotEmpty private String gitConnectorRef;
  @Trimmed @NotEmpty private String repo;
  @Trimmed @NotEmpty private String branch;
  @ApiModelProperty(allowableValues = "Github, Gitlab, Bitbucket") @NotNull private ConnectorType gitConnectorType;
  private List<GitSyncFolderConfigDTO> gitSyncFolderConfigDTOs;
}
