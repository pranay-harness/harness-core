/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.dao;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.GitSdkTestBase;
import io.harness.category.element.UnitTests;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.ChangeSets;
import io.harness.gitsync.FileProcessingResponse;
import io.harness.gitsync.FileProcessingStatus;
import io.harness.gitsync.GitToHarnessInfo;
import io.harness.gitsync.GitToHarnessProcessRequest;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import com.google.protobuf.StringValue;
import com.mongodb.client.result.UpdateResult;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GitProcessingRequestServiceImplTest extends GitSdkTestBase {
  @Inject GitProcessingRequestServiceImpl gitProcessingRequestService;

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testUpsert() {
    final GitToHarnessProcessRequest gitToHarnessProcessRequest =
        GitToHarnessProcessRequest.newBuilder()
            .setCommitId(StringValue.of("cid"))
            .setAccountId("accid")
            .setGitToHarnessBranchInfo(GitToHarnessInfo.newBuilder().setRepoUrl("url").setBranch("branch").build())
            .setChangeSets(ChangeSets.newBuilder()
                               .addChangeSet(ChangeSet.newBuilder()
                                                 .setEntityType(EntityTypeProtoEnum.CONNECTORS)
                                                 .setFilePath("./harness/abcd")
                                                 .build())
                               .build())
            .build();
    final Map<String, FileProcessingResponse> fileProcessingResponseMap =
        gitProcessingRequestService.upsert(gitToHarnessProcessRequest);
    assertThat(fileProcessingResponseMap.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void updateFileStatus() {
    final GitToHarnessProcessRequest gitToHarnessProcessRequest =
        GitToHarnessProcessRequest.newBuilder()
            .setCommitId(StringValue.of("cid"))
            .setAccountId("accid")
            .setGitToHarnessBranchInfo(GitToHarnessInfo.newBuilder().setRepoUrl("url").setBranch("branch").build())
            .setChangeSets(ChangeSets.newBuilder()
                               .addChangeSet(ChangeSet.newBuilder()
                                                 .setEntityType(EntityTypeProtoEnum.CONNECTORS)
                                                 .setFilePath("./harness/abcd")
                                                 .build())
                               .addChangeSet(ChangeSet.newBuilder()
                                                 .setEntityType(EntityTypeProtoEnum.CONNECTORS)
                                                 .setFilePath("./harness/abcde")
                                                 .build())
                               .build())
            .build();
    final Map<String, FileProcessingResponse> saved_resp =
        gitProcessingRequestService.upsert(gitToHarnessProcessRequest);

    final UpdateResult updateResult = gitProcessingRequestService.updateFileStatus(
        "cid", "./harness/abcd", FileProcessingStatus.SUCCESS, null, "accid");
    assertThat(updateResult.getMatchedCount()).isEqualTo(1);

    final Map<String, FileProcessingResponse> fileProcessingResponseMap =
        gitProcessingRequestService.upsert(gitToHarnessProcessRequest);
    assertThat(fileProcessingResponseMap.get("./harness/abcd"))
        .isEqualTo(FileProcessingResponse.newBuilder()
                       .setFilePath("./harness/abcd")
                       .setStatus(FileProcessingStatus.SUCCESS)
                       .build());
  }
}
