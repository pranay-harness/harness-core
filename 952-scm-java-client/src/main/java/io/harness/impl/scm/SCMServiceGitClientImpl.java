package io.harness.impl.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.DeleteFileRequest;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.FileBatchContentResponse;
import io.harness.product.ci.scm.proto.FileChange;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.FileModifyRequest;
import io.harness.product.ci.scm.proto.FindFilesInBranchRequest;
import io.harness.product.ci.scm.proto.FindFilesInBranchResponse;
import io.harness.product.ci.scm.proto.GetBatchFileRequest;
import io.harness.product.ci.scm.proto.GetFileRequest;
import io.harness.product.ci.scm.proto.Provider;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.product.ci.scm.proto.Signature;
import io.harness.product.ci.scm.proto.UpdateFileResponse;
import io.harness.service.ScmClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    return FileModifyRequest.newBuilder()
        .setBranch(gitFileDetails.getBranch())
        .setSlug(slug)
        .setPath(gitFileDetails.getFilePath())
        .setBranch(gitFileDetails.getBranch())
        .setContent(gitFileDetails.getFileContent())
        .setMessage(gitFileDetails.getCommitMessage())
        .setProvider(gitProvider)
        .setSignature(Signature.newBuilder()
                          .setEmail(gitFileDetails.getUserEmail())
                          .setName(gitFileDetails.getUserName())
                          .build());
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

  @Override
  public FileBatchContentResponse getHarnessFilesOfBranch(ScmConnector connector, String branch) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(connector);
    String slug = scmGitProviderHelper.getSlug(connector);
    FindFilesInBranchRequest findFilesInBranchRequest =
        FindFilesInBranchRequest.newBuilder().setBranch(branch).setSlug(slug).setProvider(gitProvider).build();
    // todo @deepak: Add the pagination logic to get the list of file names, once scm provides support
    FindFilesInBranchResponse filesInBranch = scmBlockingStub.findFilesInBranch(findFilesInBranchRequest);
    List<String> harnessRelatedFilePaths = getPathsOfFilesBelongingToHarness(filesInBranch);
    GetBatchFileRequest batchFileRequest = createBatchFileRequest(harnessRelatedFilePaths, slug, branch, gitProvider);
    return scmBlockingStub.getBatchFile(batchFileRequest);
  }

  private GetBatchFileRequest createBatchFileRequest(
      List<String> harnessRelatedFilePaths, String slug, String branch, Provider gitProvider) {
    List<GetFileRequest> getBatchFileRequests = new ArrayList<>();
    // todo @deepak: Add the pagination logic to get the list of file content, once scm provides support
    for (String path : harnessRelatedFilePaths) {
      GetFileRequest getFileRequest =
          GetFileRequest.newBuilder().setSlug(slug).setProvider(gitProvider).setBranch(branch).setPath(path).build();
      getBatchFileRequests.add(getFileRequest);
    }
    return GetBatchFileRequest.newBuilder().addAllFindRequest(getBatchFileRequests).build();
  }

  private List<String> getPathsOfFilesBelongingToHarness(FindFilesInBranchResponse filesInBranch) {
    List<FileChange> fileList = filesInBranch == null ? Collections.emptyList() : filesInBranch.getFileList();
    if (isEmpty(fileList)) {
      return Collections.emptyList();
    }
    // todo @deepak: Filter and only get files which belongs to harness
    return fileList.stream().map(fileChange -> fileChange.getPath()).collect(toList());
  }
}
