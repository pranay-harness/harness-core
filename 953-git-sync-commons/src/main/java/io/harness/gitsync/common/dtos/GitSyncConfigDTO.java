/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.dtos;

import static io.harness.NGCommonEntityConstants.ORG_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PROJECT_PARAM_MESSAGE;
import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.gitsync.sdk.GitSyncApiConstants;

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
@Schema(name = "GitSyncConfig", description = "This contains details of Git Sync Config")
@OwnedBy(DX)
@ApiModel("GitSyncConfig")
public class GitSyncConfigDTO {
  @Schema(description = GitSyncApiConstants.REPOID_PARAM_MESSAGE) @Trimmed @NotEmpty private String identifier;
  @Schema(description = GitSyncApiConstants.REPO_NAME_PARAM_MESSAGE) @Trimmed @NotEmpty private String name;
  @Schema(description = PROJECT_PARAM_MESSAGE) @Trimmed private String projectIdentifier;
  @Schema(description = ORG_PARAM_MESSAGE) @Trimmed private String orgIdentifier;
  @Schema(description = "Id of the Connector referenced in Git") @Trimmed @NotEmpty private String gitConnectorRef;
  @Schema(description = GitSyncApiConstants.REPO_URL_PARAM_MESSAGE) @Trimmed @NotEmpty private String repo;
  @Schema(description = GitSyncApiConstants.BRANCH_PARAM_MESSAGE) @Trimmed @NotEmpty private String branch;
  @Schema(description = "Connector Type")
  @ApiModelProperty(allowableValues = "Github, Gitlab, Bitbucket")
  @NotNull
  private ConnectorType gitConnectorType;
  @Schema(description = "List of all Root Folder Details") private List<GitSyncFolderConfigDTO> gitSyncFolderConfigDTOs;
}
