package io.harness.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.impl.jgit.JgitGitServiceImpl;
import io.harness.impl.scm.SCMServiceGitClientImpl;
import io.harness.product.ci.scm.proto.ContentResponse;
import io.harness.service.ScmOrchestratorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class ScmOrchestratorServiceImpl implements ScmOrchestratorService {
  private SCMServiceGitClientImpl scmServiceGitClient;
  private JgitGitServiceImpl jgitGitService;

  @Override
  public ContentResponse createFile(ScmConnector scmConnector, GitFileDetails gitFileDetails) {
    return scmServiceGitClient.createFile(scmConnector, gitFileDetails);
  }
}
