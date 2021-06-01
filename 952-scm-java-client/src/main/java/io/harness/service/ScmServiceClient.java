package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.beans.gitsync.GitPRCreateRequest;
import io.harness.beans.gitsync.GitWebhookDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.CreatePRResponse;
import io.harness.product.ci.scm.proto.CreateWebhookResponse;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.DeleteWebhookResponse;
import io.harness.product.ci.scm.proto.FileBatchContentResponse;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.FindFilesInBranchResponse;
import io.harness.product.ci.scm.proto.FindFilesInCommitResponse;
import io.harness.product.ci.scm.proto.FindFilesInPRResponse;
import io.harness.product.ci.scm.proto.GetLatestCommitResponse;
import io.harness.product.ci.scm.proto.IsLatestFileResponse;
import io.harness.product.ci.scm.proto.ListBranchesResponse;
import io.harness.product.ci.scm.proto.ListCommitsInPRResponse;
import io.harness.product.ci.scm.proto.ListCommitsResponse;
import io.harness.product.ci.scm.proto.ListWebhooksResponse;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.product.ci.scm.proto.UpdateFileResponse;

import java.util.List;

@OwnedBy(DX)
public interface ScmServiceClient {
  CreateFileResponse createFile(
      ScmConnector scmConnector, GitFileDetails gitFileDetails, SCMGrpc.SCMBlockingStub scmBlockingStub);

  UpdateFileResponse updateFile(
      ScmConnector scmConnector, GitFileDetails gitFileDetails, SCMGrpc.SCMBlockingStub scmBlockingStub);

  DeleteFileResponse deleteFile(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, SCMGrpc.SCMBlockingStub scmBlockingStub);

  FileContent getFileContent(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, SCMGrpc.SCMBlockingStub scmBlockingStub);

  FileContent getLatestFile(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, SCMGrpc.SCMBlockingStub scmBlockingStub);

  IsLatestFileResponse isLatestFile(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails,
      FileContent fileContent, SCMGrpc.SCMBlockingStub scmBlockingStub);

  FileContent pushFile(
      ScmConnector scmConnector, GitFileDetails gitFileDetails, SCMGrpc.SCMBlockingStub scmBlockingStub);

  FindFilesInBranchResponse findFilesInBranch(
      ScmConnector scmConnector, String branch, SCMGrpc.SCMBlockingStub scmBlockingStub);

  FindFilesInCommitResponse findFilesInCommit(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, SCMGrpc.SCMBlockingStub scmBlockingStub);

  FindFilesInPRResponse findFilesInPR(ScmConnector scmConnector, int prNumber, SCMGrpc.SCMBlockingStub scmBlockingStub);

  GetLatestCommitResponse getLatestCommit(
      ScmConnector scmConnector, String branch, SCMGrpc.SCMBlockingStub scmBlockingStub);

  FindFilesInCommitResponse findFilesInCommit(
      ScmConnector scmConnector, String commitHash, SCMGrpc.SCMBlockingStub scmBlockingStub);

  ListBranchesResponse listBranches(ScmConnector scmConnector, SCMGrpc.SCMBlockingStub scmBlockingStub);

  ListCommitsResponse listCommits(ScmConnector scmConnector, String branch, SCMGrpc.SCMBlockingStub scmBlockingStub);

  ListCommitsInPRResponse listCommitsInPR(
      ScmConnector scmConnector, long prNumber, SCMGrpc.SCMBlockingStub scmBlockingStub);

  FileBatchContentResponse listFiles(
      ScmConnector connector, List<String> foldersList, String branch, SCMGrpc.SCMBlockingStub scmBlockingStub);

  FileBatchContentResponse listFilesByFilePaths(
      ScmConnector connector, List<String> filePaths, String branch, SCMGrpc.SCMBlockingStub scmBlockingStub);

  void createNewBranch(
      ScmConnector scmConnector, String branch, String defaultBranchName, SCMGrpc.SCMBlockingStub scmBlockingStub);

  CreatePRResponse createPullRequest(
      ScmConnector scmConnector, GitPRCreateRequest gitPRCreateRequest, SCMGrpc.SCMBlockingStub scmBlockingStub);

  CreateWebhookResponse createWebhook(
      ScmConnector scmConnector, GitWebhookDetails gitWebhookDetails, SCMGrpc.SCMBlockingStub scmBlockingStub);

  DeleteWebhookResponse deleteWebhook(ScmConnector scmConnector, String id, SCMGrpc.SCMBlockingStub scmBlockingStub);

  ListWebhooksResponse listWebhook(ScmConnector scmConnector, SCMGrpc.SCMBlockingStub scmBlockingStub);

  CreateWebhookResponse upsertWebhook(
      ScmConnector scmConnector, GitWebhookDetails gitWebhookDetails, SCMGrpc.SCMBlockingStub scmBlockingStub);
}
