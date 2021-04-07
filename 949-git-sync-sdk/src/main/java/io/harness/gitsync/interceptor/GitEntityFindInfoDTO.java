package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Getter
@Builder
@FieldNameConstants(innerTypeName = "GitEntityFindInfoKeys")
@OwnedBy(DX)
@AllArgsConstructor
@NoArgsConstructor
public class GitEntityFindInfoDTO {
  @QueryParam(GitSyncApiConstants.BRANCH_KEY) String branch;
  @QueryParam(GitSyncApiConstants.REPO_IDENTIFIER_KEY) String yamlGitConfigId;
}
