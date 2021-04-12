package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.FileBatchContentResponse;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.FindFilesInBranchResponse;
import io.harness.product.ci.scm.proto.FindFilesInCommitResponse;
import io.harness.product.ci.scm.proto.GetLatestCommitResponse;
import io.harness.product.ci.scm.proto.IsLatestFileResponse;
import io.harness.product.ci.scm.proto.ListBranchesResponse;
import io.harness.product.ci.scm.proto.ListCommitsResponse;
import io.harness.product.ci.scm.proto.UpdateFileResponse;

import java.util.List;

@OwnedBy(DX)
public interface ScmClient {
  // It is assumed that ScmConnector is a decrypted connector.
  CreateFileResponse createFile(ScmConnector scmConnector, GitFileDetails gitFileDetails);

  UpdateFileResponse updateFile(ScmConnector scmConnector, GitFileDetails gitFileDetails);

  DeleteFileResponse deleteFile(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails);

  FileContent getFileContent(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails);

  FileBatchContentResponse getHarnessFilesOfBranch(ScmConnector connector, String branch);

  FileBatchContentResponse listFiles(ScmConnector connector, List<String> filePaths, String branch);

  FileContent getLatestFile(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails);

  IsLatestFileResponse isLatestFile(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, FileContent fileContent);

  FileContent pushFile(ScmConnector scmConnector, GitFileDetails gitFileDetails);

  FindFilesInBranchResponse findFilesInBranch(ScmConnector scmConnector, String branch);

  FindFilesInCommitResponse findFilesInCommit(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails);

  GetLatestCommitResponse getLatestCommit(ScmConnector scmConnector, String branch);

  ListBranchesResponse listBranches(ScmConnector scmConnector);

  ListCommitsResponse listCommits(ScmConnector scmConnector, String branch);

  void createNewBranch(ScmConnector scmConnector, String branch, String defaultBranchName);
}
