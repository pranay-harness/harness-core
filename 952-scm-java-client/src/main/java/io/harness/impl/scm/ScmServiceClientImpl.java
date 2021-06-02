package io.harness.impl.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.beans.gitsync.GitPRCreateRequest;
import io.harness.beans.gitsync.GitWebhookDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExplanationException;
import io.harness.exception.WingsException;
import io.harness.impl.ScmResponseStatusUtils;
import io.harness.product.ci.scm.proto.Commit;
import io.harness.product.ci.scm.proto.CompareCommitsRequest;
import io.harness.product.ci.scm.proto.CompareCommitsResponse;
import io.harness.product.ci.scm.proto.CreateBranchRequest;
import io.harness.product.ci.scm.proto.CreateBranchResponse;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.CreatePRRequest;
import io.harness.product.ci.scm.proto.CreatePRResponse;
import io.harness.product.ci.scm.proto.CreateWebhookRequest;
import io.harness.product.ci.scm.proto.CreateWebhookResponse;
import io.harness.product.ci.scm.proto.DeleteFileRequest;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.DeleteWebhookRequest;
import io.harness.product.ci.scm.proto.DeleteWebhookResponse;
import io.harness.product.ci.scm.proto.FileBatchContentResponse;
import io.harness.product.ci.scm.proto.FileChange;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.FileModifyRequest;
import io.harness.product.ci.scm.proto.FindFilesInBranchRequest;
import io.harness.product.ci.scm.proto.FindFilesInBranchResponse;
import io.harness.product.ci.scm.proto.FindFilesInCommitRequest;
import io.harness.product.ci.scm.proto.FindFilesInCommitResponse;
import io.harness.product.ci.scm.proto.FindFilesInPRRequest;
import io.harness.product.ci.scm.proto.FindFilesInPRResponse;
import io.harness.product.ci.scm.proto.GetBatchFileRequest;
import io.harness.product.ci.scm.proto.GetFileRequest;
import io.harness.product.ci.scm.proto.GetLatestCommitRequest;
import io.harness.product.ci.scm.proto.GetLatestCommitResponse;
import io.harness.product.ci.scm.proto.GetLatestFileRequest;
import io.harness.product.ci.scm.proto.IsLatestFileRequest;
import io.harness.product.ci.scm.proto.IsLatestFileResponse;
import io.harness.product.ci.scm.proto.ListBranchesRequest;
import io.harness.product.ci.scm.proto.ListBranchesResponse;
import io.harness.product.ci.scm.proto.ListCommitsInPRRequest;
import io.harness.product.ci.scm.proto.ListCommitsInPRResponse;
import io.harness.product.ci.scm.proto.ListCommitsRequest;
import io.harness.product.ci.scm.proto.ListCommitsResponse;
import io.harness.product.ci.scm.proto.ListWebhooksRequest;
import io.harness.product.ci.scm.proto.ListWebhooksResponse;
import io.harness.product.ci.scm.proto.PRFile;
import io.harness.product.ci.scm.proto.PageRequest;
import io.harness.product.ci.scm.proto.Provider;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.product.ci.scm.proto.Signature;
import io.harness.product.ci.scm.proto.UpdateFileResponse;
import io.harness.product.ci.scm.proto.WebhookResponse;
import io.harness.service.ScmServiceClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class ScmServiceClientImpl implements ScmServiceClient {
  ScmGitProviderMapper scmGitProviderMapper;
  ScmGitProviderHelper scmGitProviderHelper;

  @Override
  public CreateFileResponse createFile(
      ScmConnector scmConnector, GitFileDetails gitFileDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
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
  public UpdateFileResponse updateFile(
      ScmConnector scmConnector, GitFileDetails gitFileDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    final FileModifyRequest.Builder fileModifyRequestBuilder = getFileModifyRequest(scmConnector, gitFileDetails);
    final FileModifyRequest fileModifyRequest =
        fileModifyRequestBuilder.setBlobId(gitFileDetails.getOldFileSha()).build();
    return scmBlockingStub.updateFile(fileModifyRequest);
  }

  @Override
  public DeleteFileResponse deleteFile(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
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
  public FileContent getFileContent(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    final GetFileRequest.Builder gitFileRequestBuilder = GetFileRequest.newBuilder()
                                                             .setBranch(gitFilePathDetails.getBranch())
                                                             .setPath(gitFilePathDetails.getFilePath())
                                                             .setProvider(gitProvider)
                                                             .setSlug(slug);
    if (gitFilePathDetails.getBranch() != null) {
      gitFileRequestBuilder.setBranch(gitFilePathDetails.getBranch());
    } else if (gitFilePathDetails.getRef() != null) {
      gitFileRequestBuilder.setRef(gitFilePathDetails.getRef());
    }
    return scmBlockingStub.getFile(gitFileRequestBuilder.build());
  }

  private FileBatchContentResponse getContentOfFiles(List<String> filePaths, String slug, Provider gitProvider,
      String branch, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    GetBatchFileRequest batchFileRequest = createBatchFileRequest(filePaths, slug, branch, gitProvider);
    return scmBlockingStub.getBatchFile(batchFileRequest);
  }

  @Override
  public FileContent getLatestFile(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    GetLatestFileRequest getLatestFileRequest = getLatestFileRequestObject(scmConnector, gitFilePathDetails);
    return scmBlockingStub.getLatestFile(getLatestFileRequest);
  }

  @Override
  public IsLatestFileResponse isLatestFile(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails,
      FileContent fileContent, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    IsLatestFileRequest isLatestFileRequest = getIsLatestFileRequest(scmConnector, gitFilePathDetails, fileContent);
    return scmBlockingStub.isLatestFile(isLatestFileRequest);
  }

  @Override
  public FileContent pushFile(
      ScmConnector scmConnector, GitFileDetails gitFileDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    FileModifyRequest fileModifyRequest = getFileModifyRequest(scmConnector, gitFileDetails).build();
    return scmBlockingStub.pushFile(fileModifyRequest);
  }

  @Override
  public FindFilesInBranchResponse findFilesInBranch(
      ScmConnector scmConnector, String branch, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    FindFilesInBranchRequest findFilesInBranchRequest = getFindFilesInBranchRequest(scmConnector, branch);
    return scmBlockingStub.findFilesInBranch(findFilesInBranchRequest);
  }

  @Override
  public FindFilesInCommitResponse findFilesInCommit(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    FindFilesInCommitRequest findFilesInCommitRequest = getFindFilesInCommitRequest(scmConnector, gitFilePathDetails);
    // still to be resolved
    return scmBlockingStub.findFilesInCommit(findFilesInCommitRequest);
  }

  @Override
  public FindFilesInCommitResponse findFilesInCommit(
      ScmConnector scmConnector, String commitHash, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    FindFilesInCommitRequest findFilesInCommitRequest = getFindFilesInCommitRequest(scmConnector, commitHash);
    return scmBlockingStub.findFilesInCommit(findFilesInCommitRequest);
  }

  @Override
  public FindFilesInPRResponse findFilesInPR(
      ScmConnector scmConnector, int prNumber, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    FindFilesInPRRequest findFilesInPRRequest = getFindFilesInPRRequest(scmConnector, prNumber);
    // still to be resolved
    return scmBlockingStub.findFilesInPR(findFilesInPRRequest);
  }

  @Override
  public GetLatestCommitResponse getLatestCommit(
      ScmConnector scmConnector, String branch, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    GetLatestCommitRequest getLatestCommitRequest = getLatestCommitRequestObject(scmConnector, branch);
    return scmBlockingStub.getLatestCommit(getLatestCommitRequest);
  }

  @Override
  public ListBranchesResponse listBranches(ScmConnector scmConnector, SCMGrpc.SCMBlockingStub scmBlockingStub) {
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
  public ListCommitsResponse listCommits(
      ScmConnector scmConnector, String branch, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    ListCommitsRequest listCommitsRequest = getListCommitsRequest(scmConnector, branch);
    return scmBlockingStub.listCommits(listCommitsRequest);
  }

  @Override
  public ListCommitsInPRResponse listCommitsInPR(
      ScmConnector scmConnector, long prNumber, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    final String slug = scmGitProviderHelper.getSlug(scmConnector);
    final Provider provider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    int pageNumber = 1;
    ListCommitsInPRResponse commitsInPRResponse;
    List<Commit> commitList = new ArrayList<>();
    do {
      ListCommitsInPRRequest listCommitsInPRRequest =
          ListCommitsInPRRequest.newBuilder()
              .setSlug(slug)
              .setNumber(prNumber)
              .setProvider(provider)
              .setPagination(PageRequest.newBuilder().setPage(pageNumber).build())
              .build();
      commitsInPRResponse = scmBlockingStub.listCommitsInPR(listCommitsInPRRequest);
      commitList.addAll(commitsInPRResponse.getCommitsList());
      pageNumber = commitsInPRResponse.getPagination().getNext();
    } while (pageNumber != 0);
    return ListCommitsInPRResponse.newBuilder().addAllCommits(commitList).build();
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

  private FindFilesInPRRequest getFindFilesInPRRequest(ScmConnector scmConnector, int prNumber) {
    return FindFilesInPRRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setNumber(prNumber)
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

  private FindFilesInCommitRequest getFindFilesInCommitRequest(ScmConnector scmConnector, String commitHash) {
    return FindFilesInCommitRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setRef(commitHash)
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

  private ListBranchesRequest getListBranchesRequest(ScmConnector scmConnector) {
    return ListBranchesRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
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

  private ListCommitsInPRRequest getListCommitsInPRRequest(ScmConnector scmConnector, long prNumber) {
    return ListCommitsInPRRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setNumber(prNumber)
        .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector))
        .build();
  }

  @Override
  public FileBatchContentResponse listFiles(
      ScmConnector connector, List<String> foldersList, String branchName, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(connector);
    String slug = scmGitProviderHelper.getSlug(connector);
    List<String> getFilesWhichArePartOfHarness =
        getFileNames(foldersList, slug, gitProvider, branchName, scmBlockingStub);
    return getContentOfFiles(getFilesWhichArePartOfHarness, slug, gitProvider, branchName, scmBlockingStub);
  }

  private List<String> getFileNames(List<String> foldersList, String slug, Provider gitProvider, String branchName,
      SCMGrpc.SCMBlockingStub scmBlockingStub) {
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
  public FileBatchContentResponse listFilesByFilePaths(
      ScmConnector connector, List<String> filePaths, String branch, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(connector);
    String slug = scmGitProviderHelper.getSlug(connector);
    return getContentOfFiles(filePaths, slug, gitProvider, branch, scmBlockingStub);
  }

  @Override
  public void createNewBranch(
      ScmConnector scmConnector, String branch, String defaultBranchName, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    String latestShaOfBranch = getLatestShaOfBranch(slug, gitProvider, defaultBranchName, scmBlockingStub);
    final CreateBranchResponse createBranchResponse =
        createNewBranchFromDefault(slug, gitProvider, branch, latestShaOfBranch, scmBlockingStub);
    ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(createBranchResponse.getStatus(), null);
  }

  @Override
  public CreatePRResponse createPullRequest(
      ScmConnector scmConnector, GitPRCreateRequest gitPRCreateRequest, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    CreatePRRequest createPRRequest = CreatePRRequest.newBuilder()
                                          .setSlug(slug)
                                          .setTitle(gitPRCreateRequest.getTitle())
                                          .setProvider(gitProvider)
                                          .setSource(gitPRCreateRequest.getSourceBranch())
                                          .setTarget(gitPRCreateRequest.getTargetBranch())
                                          .build();
    final CreatePRResponse prResponse = scmBlockingStub.createPR(createPRRequest);
    try {
      ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(prResponse.getStatus(), null);
    } catch (WingsException e) {
      if (ErrorCode.SCM_NOT_MODIFIED.equals(e.getCode())) {
        throw new ExplanationException("A PR already exist for given branches", e);
      }
      throw e;
    }
    return prResponse;
  }

  @Override
  public CreateWebhookResponse createWebhook(
      ScmConnector scmConnector, GitWebhookDetails gitWebhookDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    CreateWebhookRequest createWebhookRequest =
        getCreateWebhookRequest(slug, gitProvider, gitWebhookDetails, scmConnector, null);
    return scmBlockingStub.createWebhook(createWebhookRequest);
  }

  public CreateWebhookResponse createWebhook(ScmConnector scmConnector, GitWebhookDetails gitWebhookDetails,
      SCMGrpc.SCMBlockingStub scmBlockingStub, WebhookResponse exisitingWebhook) {
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    CreateWebhookRequest createWebhookRequest =
        getCreateWebhookRequest(slug, gitProvider, gitWebhookDetails, scmConnector, exisitingWebhook);
    return scmBlockingStub.createWebhook(createWebhookRequest);
  }

  private CreateWebhookRequest getCreateWebhookRequest(String slug, Provider gitProvider,
      GitWebhookDetails gitWebhookDetails, ScmConnector scmConnector, WebhookResponse identicalTarget) {
    final CreateWebhookRequest.Builder createWebhookRequestBuilder = CreateWebhookRequest.newBuilder()
                                                                         .setSlug(slug)
                                                                         .setProvider(gitProvider)
                                                                         .setTarget(gitWebhookDetails.getTarget());
    return ScmGitWebhookHelper.getCreateWebhookRequest(
        createWebhookRequestBuilder, gitWebhookDetails, scmConnector, identicalTarget);
  }

  @Override
  public DeleteWebhookResponse deleteWebhook(
      ScmConnector scmConnector, String id, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    DeleteWebhookRequest deleteWebhookRequest = getDeleteWebhookRequest(slug, gitProvider, id);
    return scmBlockingStub.deleteWebhook(deleteWebhookRequest);
  }

  private DeleteWebhookRequest getDeleteWebhookRequest(String slug, Provider gitProvider, String id) {
    return DeleteWebhookRequest.newBuilder().setSlug(slug).setProvider(gitProvider).setId(id).build();
  }

  @Override
  public ListWebhooksResponse listWebhook(ScmConnector scmConnector, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    ListWebhooksRequest listWebhooksRequest = getListWebhookRequest(slug, gitProvider);
    return scmBlockingStub.listWebhooks(listWebhooksRequest);
  }

  @Override
  public CreateWebhookResponse upsertWebhook(
      ScmConnector scmConnector, GitWebhookDetails gitWebhookDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    ListWebhooksResponse listWebhooksResponse = listWebhook(scmConnector, scmBlockingStub);
    final List<WebhookResponse> webhooksList = listWebhooksResponse.getWebhooksList();
    WebhookResponse existingWebhook = null;
    for (WebhookResponse webhookResponse : webhooksList) {
      if (isIdentical(webhookResponse, gitWebhookDetails, scmConnector)) {
        return CreateWebhookResponse.newBuilder().build();
      }
      if (isIdenticalTarget(webhookResponse, gitWebhookDetails)) {
        existingWebhook = webhookResponse;
        final DeleteWebhookResponse deleteWebhookResponse =
            deleteWebhook(scmConnector, webhookResponse.getId(), scmBlockingStub);
        ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(deleteWebhookResponse.getStatus(), null);
      }
    }
    CreateWebhookResponse createWebhookResponse =
        createWebhook(scmConnector, gitWebhookDetails, scmBlockingStub, existingWebhook);
    ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(createWebhookResponse.getStatus(), null);
    return createWebhookResponse;
  }

  @Override
  public CompareCommitsResponse compareCommits(ScmConnector scmConnector, String initialCommitId, String finalCommitId,
      SCMGrpc.SCMBlockingStub scmBlockingStub) {
    List<PRFile> prFiles = new ArrayList<>();
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    CompareCommitsRequest.Builder request = CompareCommitsRequest.newBuilder()
                                                .setSource(initialCommitId)
                                                .setTarget(finalCommitId)
                                                .setSlug(slug)
                                                .setProvider(gitProvider)
                                                .setPagination(PageRequest.newBuilder().setPage(1).build());
    CompareCommitsResponse response;
    // process request in pagination manner
    do {
      response = scmBlockingStub.compareCommits(request.build());
      prFiles.addAll(response.getFilesList());
      // Set next page in the request
      request.setPagination(PageRequest.newBuilder().setPage(response.getPagination().getNext()).build());
    } while (response.getPagination().getNext() != 0);

    return CompareCommitsResponse.newBuilder().addAllFiles(prFiles).build();
  }

  private ListWebhooksRequest getListWebhookRequest(String slug, Provider gitProvider) {
    // Do pagination if webhook goes beyond single page
    return ListWebhooksRequest.newBuilder()
        .setSlug(slug)
        .setProvider(gitProvider)
        .setPagination(PageRequest.newBuilder().setPage(1).build())
        .build();
  }

  private CreateBranchResponse createNewBranchFromDefault(String slug, Provider gitProvider, String branch,
      String latestShaOfBranch, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    return scmBlockingStub.createBranch(CreateBranchRequest.newBuilder()
                                            .setName(branch)
                                            .setCommitId(latestShaOfBranch)
                                            .setProvider(gitProvider)
                                            .setSlug(slug)
                                            .build());
  }

  private String getLatestShaOfBranch(
      String slug, Provider gitProvider, String defaultBranchName, SCMGrpc.SCMBlockingStub scmBlockingStub) {
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

  private boolean isIdenticalTarget(WebhookResponse webhookResponse, GitWebhookDetails gitWebhookDetails) {
    return webhookResponse.getTarget().equals(gitWebhookDetails.getTarget());
  }

  private boolean isIdentical(
      WebhookResponse webhookResponse, GitWebhookDetails gitWebhookDetails, ScmConnector scmConnector) {
    return isIdenticalTarget(webhookResponse, gitWebhookDetails)
        && ScmGitWebhookHelper.isIdenticalEvents(webhookResponse, gitWebhookDetails.getHookEventType(), scmConnector);
  }
}
