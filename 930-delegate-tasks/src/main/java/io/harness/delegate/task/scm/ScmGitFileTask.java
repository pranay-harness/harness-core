/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FileContentBatchResponse;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.service.ScmServiceClient;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.commons.lang3.NotImplementedException;

@OwnedBy(HarnessTeam.DX)
public class ScmGitFileTask extends AbstractDelegateRunnableTask {
  @Inject ScmDelegateClient scmDelegateClient;
  @Inject ScmServiceClient scmServiceClient;
  @Inject SecretDecryptionService secretDecryptionService;

  public ScmGitFileTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    ScmGitFileTaskParams scmGitFileTaskParams = (ScmGitFileTaskParams) parameters;
    secretDecryptionService.decrypt(
        GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(scmGitFileTaskParams.getScmConnector()),
        scmGitFileTaskParams.getEncryptedDataDetails());
    switch (scmGitFileTaskParams.getGitFileTaskType()) {
      case GET_FILE_CONTENT_BATCH:
        FileContentBatchResponse fileBatchContentResponse = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.listFiles(scmGitFileTaskParams.getScmConnector(), scmGitFileTaskParams.getFoldersList(),
                scmGitFileTaskParams.getBranch(), SCMGrpc.newBlockingStub(c)));
        return GitFileTaskResponseData.builder()
            .gitFileTaskType(scmGitFileTaskParams.getGitFileTaskType())
            .fileBatchContentResponse(fileBatchContentResponse.getFileBatchContentResponse().toByteArray())
            .commitId(fileBatchContentResponse.getCommitId())
            .build();
      case GET_FILE_CONTENT:
        final FileContent fileContent = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.getFileContent(scmGitFileTaskParams.getScmConnector(),
                scmGitFileTaskParams.getGitFilePathDetails(), SCMGrpc.newBlockingStub(c)));
        return GitFileTaskResponseData.builder()
            .gitFileTaskType(scmGitFileTaskParams.getGitFileTaskType())
            .fileContent(fileContent.toByteArray())
            .build();
      case GET_FILE_CONTENT_BATCH_BY_FILE_PATHS:
        fileBatchContentResponse = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.listFilesByFilePaths(scmGitFileTaskParams.getScmConnector(),
                scmGitFileTaskParams.getFilePathsList(), scmGitFileTaskParams.getBranch(), SCMGrpc.newBlockingStub(c)));
        return GitFileTaskResponseData.builder()
            .gitFileTaskType(scmGitFileTaskParams.getGitFileTaskType())
            .fileBatchContentResponse(fileBatchContentResponse.getFileBatchContentResponse().toByteArray())
            .commitId(fileBatchContentResponse.getCommitId())
            .build();
      case GET_FILE_CONTENT_BATCH_BY_REF:
        fileBatchContentResponse = scmDelegateClient.processScmRequest(c
            -> scmServiceClient.listFilesByCommitId(scmGitFileTaskParams.getScmConnector(),
                scmGitFileTaskParams.getFilePathsList(), scmGitFileTaskParams.getRef(), SCMGrpc.newBlockingStub(c)));
        return GitFileTaskResponseData.builder()
            .gitFileTaskType(scmGitFileTaskParams.getGitFileTaskType())
            .fileBatchContentResponse(fileBatchContentResponse.getFileBatchContentResponse().toByteArray())
            .commitId(fileBatchContentResponse.getCommitId())
            .build();
      default:
        throw new UnknownEnumTypeException("GitFileTaskType", scmGitFileTaskParams.getGitFileTaskType().toString());
    }
  }

  @Override
  public boolean isSupportingErrorFramework() {
    return true;
  }
}
