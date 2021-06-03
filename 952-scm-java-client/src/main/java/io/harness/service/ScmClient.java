package io.harness.service;

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
import io.harness.product.ci.scm.proto.UpdateFileResponse;

import java.util.List;

@OwnedBy(DX)
public interface ScmClient {
  // It is assumed that ScmConnector is a decrypted connector.
  CreateFileResponse createFile(ScmConnector scmConnector, GitFileDetails gitFileDetails);

  UpdateFileResponse updateFile(ScmConnector scmConnector, GitFileDetails gitFileDetails);

  DeleteFileResponse deleteFile(ScmConnector scmConnector, GitFileDetails gitFileDetails);

  FileContent getFileContent(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails);

  FileContent getLatestFile(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails);

  IsLatestFileResponse isLatestFile(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, FileContent fileContent);

  FileContent pushFile(ScmConnector scmConnector, GitFileDetails gitFileDetails);

  FindFilesInBranchResponse findFilesInBranch(ScmConnector scmConnector, String branchName);

  FindFilesInCommitResponse findFilesInCommit(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails);

  GetLatestCommitResponse getLatestCommit(ScmConnector scmConnector, String branchName);

  ListBranchesResponse listBranches(ScmConnector scmConnector);

  ListCommitsResponse listCommits(ScmConnector scmConnector, String branchName);

  ListCommitsInPRResponse listCommitsInPR(ScmConnector scmConnector, int prNumber);

  FileBatchContentResponse listFiles(ScmConnector connector, List<String> foldersList, String branchName);

  FileBatchContentResponse listFilesByFilePaths(ScmConnector connector, List<String> filePathsList, String branchName);

  void createNewBranch(ScmConnector scmConnector, String branch, String defaultBranchName);

  CreatePRResponse createPullRequest(ScmConnector scmConnector, GitPRCreateRequest gitPRCreateRequest);

  CreateWebhookResponse createWebhook(ScmConnector scmConnector, GitWebhookDetails gitWebhookDetails);

  DeleteWebhookResponse deleteWebhook(ScmConnector scmConnector, String id);

  ListWebhooksResponse listWebhook(ScmConnector scmConnector);

  CreateWebhookResponse upsertWebhook(ScmConnector scmConnector, GitWebhookDetails gitWebhookDetails);

  CompareCommitsResponse compareCommits(ScmConnector scmConnector, String initialCommitId, String finalCommitId);
}
