package io.harness.impl.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.FileModifyRequest;
import io.harness.product.ci.scm.proto.Provider;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.product.ci.scm.proto.Signature;
import io.harness.service.ScmClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class SCMServiceGitClientImpl implements ScmClient {
  SCMGrpc.SCMBlockingStub scmBlockingStub;
  ScmGitProviderMapper scmGitProviderMapper;
  ScmGitProviderHelper scmGitProviderHelper;

  @Override
  public CreateFileResponse createFile(ScmConnector scmConnector, GitFileDetails gitFileDetails) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    // todo @deepak: To run the request please add username and email in the signature
    FileModifyRequest fileModifyRequest = FileModifyRequest.newBuilder()
                                              .setBranch(gitFileDetails.getBranch())
                                              .setSlug(slug)
                                              .setPath(gitFileDetails.getFilePath())
                                              .setBranch(gitFileDetails.getBranch())
                                              .setContent(gitFileDetails.getFileContent())
                                              .setMessage(gitFileDetails.getCommitMessage())
                                              .setProvider(gitProvider)
                                              .setSignature(Signature.newBuilder().build())
                                              .build();
    return scmBlockingStub.createFile(fileModifyRequest);
  }
}
