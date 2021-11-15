package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Getter
@Builder
@FieldNameConstants(innerTypeName = "GitEntityFindInfoKeys")
@Schema(name = "GitEntityFindInfo", description = "Details to find Git Entity including: Git Config Id and Branch name")
@OwnedBy(DX)
@NoArgsConstructor
@AllArgsConstructor
public class GitEntityFindInfoDTO {
  @QueryParam(GitSyncApiConstants.BRANCH_KEY) String branch;
  @QueryParam(GitSyncApiConstants.REPO_IDENTIFIER_KEY) String yamlGitConfigId;
  @QueryParam(GitSyncApiConstants.DEFAULT_FROM_OTHER_REPO) Boolean defaultFromOtherRepo;
}
