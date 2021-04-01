package io.harness.impl.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.DeleteFileRequest;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.FileModifyRequest;
import io.harness.product.ci.scm.proto.GetFileRequest;
import io.harness.product.ci.scm.proto.Provider;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.product.ci.scm.proto.Signature;
import io.harness.product.ci.scm.proto.UpdateFileResponse;
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
    FileModifyRequest fileModifyRequest = getFileModifyRequest(scmConnector, gitFileDetails).build();
    return scmBlockingStub.createFile(fileModifyRequest);
  }

  private FileModifyRequest.Builder getFileModifyRequest(ScmConnector scmConnector, GitFileDetails gitFileDetails) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    // todo @deepak: write helper util to extract username from connector
    return FileModifyRequest.newBuilder()
        .setBranch(gitFileDetails.getBranch())
        .setSlug(slug)
        .setPath(gitFileDetails.getFilePath())
        .setBranch(gitFileDetails.getBranch())
        .setContent(gitFileDetails.getFileContent())
        .setMessage(gitFileDetails.getCommitMessage())
        .setProvider(gitProvider)
        .setSignature(Signature.newBuilder().setEmail(gitFileDetails.getUserEmail()).setName("test").build());
  }

  @Override
  public UpdateFileResponse updateFile(ScmConnector scmConnector, GitFileDetails gitFileDetails) {
    final FileModifyRequest.Builder fileModifyRequestBuilder = getFileModifyRequest(scmConnector, gitFileDetails);
    final FileModifyRequest fileModifyRequest = fileModifyRequestBuilder.setSha(gitFileDetails.getOldFileSha()).build();
    return scmBlockingStub.updateFile(fileModifyRequest);
  }

  @Override
  public DeleteFileResponse deleteFile(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    final DeleteFileRequest deleteFileRequest = DeleteFileRequest.newBuilder()
                                                    .setBranch(gitFilePathDetails.getBranch())
                                                    .setPath(gitFilePathDetails.getFilePath())
                                                    .setProvider(gitProvider)
                                                    .setSlug(slug)
                                                    .build();

    return scmBlockingStub.deleteFile(deleteFileRequest);
  }

  @Override
  public FileContent getFileContent(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    final GetFileRequest fileRequest = GetFileRequest.newBuilder()
                                           .setBranch(gitFilePathDetails.getBranch())
                                           .setPath(gitFilePathDetails.getFilePath())
                                           .setProvider(gitProvider)
                                           .setSlug(slug)
                                           .build();
    return scmBlockingStub.getFile(fileRequest);
  }
}
