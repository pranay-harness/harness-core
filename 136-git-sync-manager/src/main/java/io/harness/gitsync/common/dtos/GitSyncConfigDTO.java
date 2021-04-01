package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
import io.harness.delegate.beans.connector.ConnectorType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(DX)
@ApiModel("GitSyncConfig")
public class GitSyncConfigDTO {
  private String identifier;
  @Trimmed private String projectIdentifier;
  @Trimmed private String orgIdentifier;
  @Trimmed @NotEmpty private String gitConnectorRef;
  @Trimmed @NotEmpty private String repo;
  @Trimmed @NotEmpty private String branch;
  private String name;
  @ApiModelProperty(allowableValues = "Github, Gitlab, Bitbucket") @NotNull private ConnectorType gitConnectorType;
  private List<GitSyncFolderConfigDTO> gitSyncFolderConfigDTOs;
}
