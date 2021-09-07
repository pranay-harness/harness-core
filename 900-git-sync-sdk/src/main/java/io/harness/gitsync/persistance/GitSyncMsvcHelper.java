package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.HarnessToGitPushInfoServiceGrpc.HarnessToGitPushInfoServiceBlockingStub;
import io.harness.gitsync.PushInfo;
import io.harness.gitsync.PushResponse;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.scm.beans.ScmPushResponse;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailRestToProtoMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Singleton
@Slf4j
@OwnedBy(DX)
public class GitSyncMsvcHelper {
  @Inject private HarnessToGitPushInfoServiceBlockingStub harnessToGitPushInfoServiceBlockingStub;
  @Inject private EntityDetailRestToProtoMapper entityDetailRestToProtoMapper;

  public void postPushInformationToGitMsvc(
      EntityDetail entityDetail, ScmPushResponse scmResponse, GitEntityInfo gitBranchInfo) {
    final PushResponse pushResponse = harnessToGitPushInfoServiceBlockingStub.pushFromHarness(
        PushInfo.newBuilder()
            .setAccountId(entityDetail.getEntityRef().getAccountIdentifier())
            .setCommitId(scmResponse.getCommitId())
            .setEntityDetail(entityDetailRestToProtoMapper.createEntityDetailDTO(entityDetail))
            .setFolderPath(scmResponse.getFolderPath())
            .setFilePath(scmResponse.getFilePath())
            .setYamlGitConfigId(scmResponse.getYamlGitConfigId())
            .setBranchName(gitBranchInfo.getBranch())
            .setIsNewBranch(checkIfItsANewBranch(gitBranchInfo))
            .putAllContextMap(MDC.getCopyOfContextMap())
            .setIsSyncFromGit(gitBranchInfo.isSyncFromGit())
            .build());
    log.info("Posted information to git sync manager for commit id: [{}], entity detail: [{}]",
        scmResponse.getCommitId(), entityDetail);
  }

  private boolean checkIfItsANewBranch(GitEntityInfo gitBranchInfo) {
    if (gitBranchInfo.isSyncFromGit()) {
      return false;
    }
    return gitBranchInfo.isNewBranch();
  }
}
