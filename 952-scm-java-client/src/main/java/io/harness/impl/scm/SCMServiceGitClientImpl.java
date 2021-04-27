package io.harness.impl.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.product.ci.scm.proto.CreateBranchRequest;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.DeleteFileRequest;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.FileBatchContentResponse;
import io.harness.product.ci.scm.proto.FileChange;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.FileModifyRequest;
import io.harness.product.ci.scm.proto.FindFilesInBranchRequest;
import io.harness.product.ci.scm.proto.FindFilesInBranchResponse;
import io.harness.product.ci.scm.proto.FindFilesInCommitRequest;
import io.harness.product.ci.scm.proto.FindFilesInCommitResponse;
import io.harness.product.ci.scm.proto.GetBatchFileRequest;
import io.harness.product.ci.scm.proto.GetFileRequest;
import io.harness.product.ci.scm.proto.GetLatestCommitRequest;
import io.harness.product.ci.scm.proto.GetLatestCommitResponse;
import io.harness.product.ci.scm.proto.GetLatestFileRequest;
import io.harness.product.ci.scm.proto.IsLatestFileRequest;
import io.harness.product.ci.scm.proto.IsLatestFileResponse;
import io.harness.product.ci.scm.proto.ListBranchesRequest;
import io.harness.product.ci.scm.proto.ListBranchesResponse;
import io.harness.product.ci.scm.proto.ListCommitsRequest;
import io.harness.product.ci.scm.proto.ListCommitsResponse;
import io.harness.product.ci.scm.proto.PageRequest;
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
    final FileModifyRequest fileModifyRequest =
        fileModifyRequestBuilder.setBlobId(gitFileDetails.getOldFileSha()).build();
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

  private FileBatchContentResponse getContentOfFiles(
      List<String> filePaths, String slug, Provider gitProvider, String branch) {
    GetBatchFileRequest batchFileRequest = createBatchFileRequest(filePaths, slug, branch, gitProvider);
    return scmBlockingStub.getBatchFile(batchFileRequest);
  }

  private List<String> getFileNames(List<String> foldersList, String slug, Provider gitProvider, String branchName) {
    GetFilesInFolderForkTask getFilesInFolderTask = GetFilesInFolderForkTask.builder()
                                                        .branch(branchName)
                                                        .provider(gitProvider)
                                                        .scmBlockingStub(scmBlockingStub)
                                                        .slug(slug)
                                                        .build();
    List<FileChange> forkJoinTask = getFilesInFolderTask.createForkJoinTask(foldersList);
    return emptyIfNull(forkJoinTask).stream().map(FileChange::getPath).collect(toList());
  }

  @Override
  public FileContent getLatestFile(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails) {
    GetLatestFileRequest getLatestFileRequest = getLatestFileRequestObject(scmConnector, gitFilePathDetails);
    return scmBlockingStub.getLatestFile(getLatestFileRequest);
  }

  @Override
  public IsLatestFileResponse isLatestFile(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, FileContent fileContent) {
    IsLatestFileRequest isLatestFileRequest = getIsLatestFileRequest(scmConnector, gitFilePathDetails, fileContent);
    return scmBlockingStub.isLatestFile(isLatestFileRequest);
  }

  @Override
  public FileContent pushFile(ScmConnector scmConnector, GitFileDetails gitFileDetails) {
    FileModifyRequest fileModifyRequest = getFileModifyRequest(scmConnector, gitFileDetails).build();
    return scmBlockingStub.pushFile(fileModifyRequest);
  }

  @Override
  public FindFilesInBranchResponse findFilesInBranch(ScmConnector scmConnector, String branchName) {
    FindFilesInBranchRequest findFilesInBranchRequest = getFindFilesInBranchRequest(scmConnector, branchName);
    return scmBlockingStub.findFilesInBranch(findFilesInBranchRequest);
  }

  @Override
  public FindFilesInCommitResponse findFilesInCommit(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails) {
    FindFilesInCommitRequest findFilesInCommitRequest = getFindFilesInCommitRequest(scmConnector, gitFilePathDetails);
    // still to be resolved
    return scmBlockingStub.findFilesInCommit(findFilesInCommitRequest);
  }

  @Override
  public GetLatestCommitResponse getLatestCommit(ScmConnector scmConnector, String branchName) {
    GetLatestCommitRequest getLatestCommitRequest = getLatestCommitRequestObject(scmConnector, branchName);
    return scmBlockingStub.getLatestCommit(getLatestCommitRequest);
  }

  @Override
  public ListBranchesResponse listBranches(ScmConnector scmConnector) {
    final String slug = scmGitProviderHelper.getSlug(scmConnector);
    final Provider provider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    int pageNumber = 1;
    ListBranchesResponse branchList = null;
    List<String> branchesList = new ArrayList<>();
    do {
      ListBranchesRequest listBranchesRequest = ListBranchesRequest.newBuilder()
                                                    .setSlug(slug)
                                                    .setProvider(provider)
                                                    .setPagination(PageRequest.newBuilder().setPage(pageNumber).build())
                                                    .build();
      branchList = scmBlockingStub.listBranches(listBranchesRequest);
      branchesList.addAll(branchList.getBranchesList());
      pageNumber = branchList.getPagination().getNext();
    } while (hasMoreBranches(branchList));
    return ListBranchesResponse.newBuilder().addAllBranches(branchesList).build();
  }

  private boolean hasMoreBranches(ListBranchesResponse branchList) {
    return branchList != null && branchList.getPagination() != null && branchList.getPagination().getNext() != 0;
  }

  @Override
  public ListCommitsResponse listCommits(ScmConnector scmConnector, String branchName) {
    ListCommitsRequest listCommitsRequest = getListCommitsRequest(scmConnector, branchName);
    return scmBlockingStub.listCommits(listCommitsRequest);
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

  private GetLatestFileRequest getLatestFileRequestObject(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails) {
    return GetLatestFileRequest.newBuilder()
        .setBranch(gitFilePathDetails.getBranch())
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector))
        .setPath(gitFilePathDetails.getFilePath())
        .build();
  }

  private IsLatestFileRequest getIsLatestFileRequest(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, FileContent fileContent) {
    return IsLatestFileRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setPath(gitFilePathDetails.getFilePath())
        .setBranch(gitFilePathDetails.getBranch())
        .setBlobId(fileContent.getBlobId())
        .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector))
        .build();
  }

  private FindFilesInBranchRequest getFindFilesInBranchRequest(ScmConnector scmConnector, String branch) {
    return FindFilesInBranchRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setBranch(branch)
        .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector))
        .build();
  }

  private FindFilesInCommitRequest getFindFilesInCommitRequest(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails) {
    return FindFilesInCommitRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setRef(gitFilePathDetails.getBranch()) // How to get Ref for files????????????
        .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector))
        .build();
  }

  private GetLatestCommitRequest getLatestCommitRequestObject(ScmConnector scmConnector, String branch) {
    return GetLatestCommitRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setBranch(branch)
        .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector))
        .build();
  }

  private ListCommitsRequest getListCommitsRequest(ScmConnector scmConnector, String branch) {
    return ListCommitsRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setBranch(branch)
        .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector))
        .build();
  }

  @Override
  public FileBatchContentResponse listFiles(ScmConnector connector, List<String> foldersList, String branchName) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(connector);
    String slug = scmGitProviderHelper.getSlug(connector);
    List<String> getFilesWhichArePartOfHarness = getFileNames(foldersList, slug, gitProvider, branchName);
    return getContentOfFiles(getFilesWhichArePartOfHarness, slug, gitProvider, branchName);
  }

  @Override
  public void createNewBranch(ScmConnector scmConnector, String branch, String defaultBranchName) {
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    String latestShaOfBranch = getLatestShaOfBranch(slug, gitProvider, defaultBranchName);
    createNewBranchFromDefault(slug, gitProvider, branch, latestShaOfBranch);
  }

  private void createNewBranchFromDefault(String slug, Provider gitProvider, String branch, String latestShaOfBranch) {
    scmBlockingStub.createBranch(CreateBranchRequest.newBuilder()
                                     .setName(branch)
                                     .setCommitId(latestShaOfBranch)
                                     .setProvider(gitProvider)
                                     .setSlug(slug)
                                     .build());
  }

  private String getLatestShaOfBranch(String slug, Provider gitProvider, String defaultBranchName) {
    GetLatestCommitResponse latestCommit = scmBlockingStub.getLatestCommit(GetLatestCommitRequest.newBuilder()
                                                                               .setBranch(defaultBranchName)
                                                                               .setSlug(slug)
                                                                               .setProvider(gitProvider)
                                                                               .build());
    return latestCommit.getCommitId();
  }

  private String getGithubToken(Provider gitProvider) {
    return gitProvider.getGithub().getAccessToken();
  }
}
