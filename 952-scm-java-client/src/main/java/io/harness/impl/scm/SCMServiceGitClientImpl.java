package io.harness.impl.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.beans.gitsync.GitPRCreateRequest;
import io.harness.beans.gitsync.GitWebhookDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.product.ci.scm.proto.CompareCommitsResponse;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.CreatePRResponse;
import io.harness.product.ci.scm.proto.CreateWebhookResponse;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.DeleteWebhookResponse;
import io.harness.product.ci.scm.proto.FileBatchContentResponse;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.FindFilesInBranchResponse;
import io.harness.product.ci.scm.proto.FindFilesInCommitResponse;
import io.harness.product.ci.scm.proto.GetLatestCommitResponse;
import io.harness.product.ci.scm.proto.IsLatestFileResponse;
import io.harness.product.ci.scm.proto.ListBranchesResponse;
import io.harness.product.ci.scm.proto.ListCommitsInPRResponse;
import io.harness.product.ci.scm.proto.ListCommitsResponse;
import io.harness.product.ci.scm.proto.ListWebhooksResponse;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.product.ci.scm.proto.UpdateFileResponse;
import io.harness.service.ScmClient;
import io.harness.service.ScmServiceClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
  ScmServiceClient scmServiceClient;

  @Override
  public CreateFileResponse createFile(ScmConnector scmConnector, GitFileDetails gitFileDetails) {
    return scmServiceClient.createFile(scmConnector, gitFileDetails, scmBlockingStub);
  }

  @Override
  public UpdateFileResponse updateFile(ScmConnector scmConnector, GitFileDetails gitFileDetails) {
    return scmServiceClient.updateFile(scmConnector, gitFileDetails, scmBlockingStub);
  }

  @Override
  public DeleteFileResponse deleteFile(ScmConnector scmConnector, GitFileDetails gitFileDetails) {
    return scmServiceClient.deleteFile(scmConnector, gitFileDetails, scmBlockingStub);
  }

  @Override
  public FileContent getFileContent(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails) {
    return scmServiceClient.getFileContent(scmConnector, gitFilePathDetails, scmBlockingStub);
  }

  @Override
  public FileContent getLatestFile(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails) {
    return scmServiceClient.getLatestFile(scmConnector, gitFilePathDetails, scmBlockingStub);
  }

  @Override
  public IsLatestFileResponse isLatestFile(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, FileContent fileContent) {
    return scmServiceClient.isLatestFile(scmConnector, gitFilePathDetails, fileContent, scmBlockingStub);
  }

  @Override
  public FileContent pushFile(ScmConnector scmConnector, GitFileDetails gitFileDetails) {
    return scmServiceClient.pushFile(scmConnector, gitFileDetails, scmBlockingStub);
  }

  @Override
  public FindFilesInBranchResponse findFilesInBranch(ScmConnector scmConnector, String branch) {
    return scmServiceClient.findFilesInBranch(scmConnector, branch, scmBlockingStub);
  }

  @Override
  public FindFilesInCommitResponse findFilesInCommit(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails) {
    return scmServiceClient.findFilesInCommit(scmConnector, gitFilePathDetails, scmBlockingStub);
  }

  @Override
  public GetLatestCommitResponse getLatestCommit(ScmConnector scmConnector, String branch) {
    return scmServiceClient.getLatestCommit(scmConnector, branch, scmBlockingStub);
  }

  @Override
  public ListBranchesResponse listBranches(ScmConnector scmConnector) {
    return scmServiceClient.listBranches(scmConnector, scmBlockingStub);
  }

  @Override
  public ListCommitsResponse listCommits(ScmConnector scmConnector, String branch) {
    return scmServiceClient.listCommits(scmConnector, branch, scmBlockingStub);
  }

  @Override
  public ListCommitsInPRResponse listCommitsInPR(ScmConnector scmConnector, int prNumber) {
    return scmServiceClient.listCommitsInPR(scmConnector, prNumber, scmBlockingStub);
  }

  @Override
  public FileBatchContentResponse listFiles(ScmConnector connector, List<String> foldersList, String branch) {
    return scmServiceClient.listFiles(connector, foldersList, branch, scmBlockingStub);
  }

  @Override
  public FileBatchContentResponse listFilesByFilePaths(
      ScmConnector connector, List<String> filePathsList, String branchName) {
    return scmServiceClient.listFilesByFilePaths(connector, filePathsList, branchName, scmBlockingStub);
  }

  @Override
  public void createNewBranch(ScmConnector scmConnector, String branch, String defaultBranchName) {
    scmServiceClient.createNewBranch(scmConnector, branch, defaultBranchName, scmBlockingStub);
  }

  @Override
  public CreatePRResponse createPullRequest(ScmConnector scmConnector, GitPRCreateRequest gitPRCreateRequest) {
    return scmServiceClient.createPullRequest(scmConnector, gitPRCreateRequest, scmBlockingStub);
  }

  @Override
  public CreateWebhookResponse createWebhook(ScmConnector scmConnector, GitWebhookDetails gitWebhookDetails) {
    return scmServiceClient.createWebhook(scmConnector, gitWebhookDetails, scmBlockingStub);
  }

  @Override
  public DeleteWebhookResponse deleteWebhook(ScmConnector scmConnector, String id) {
    return scmServiceClient.deleteWebhook(scmConnector, id, scmBlockingStub);
  }

  @Override
  public ListWebhooksResponse listWebhook(ScmConnector scmConnector) {
    return scmServiceClient.listWebhook(scmConnector, scmBlockingStub);
  }

  @Override
  public CreateWebhookResponse upsertWebhook(ScmConnector scmConnector, GitWebhookDetails gitWebhookDetails) {
    return scmServiceClient.upsertWebhook(scmConnector, gitWebhookDetails, scmBlockingStub);
  }

  @Override
  public CompareCommitsResponse compareCommits(
      ScmConnector scmConnector, String initialCommitId, String finalCommitId) {
    return scmServiceClient.compareCommits(scmConnector, initialCommitId, finalCommitId, scmBlockingStub);
  }
}
