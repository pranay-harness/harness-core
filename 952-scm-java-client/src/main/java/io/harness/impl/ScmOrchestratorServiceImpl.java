package io.harness.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.impl.jgit.JgitGitServiceImpl;
import io.harness.impl.scm.SCMServiceGitClientImpl;
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
import io.harness.service.ScmOrchestratorService;

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
public class ScmOrchestratorServiceImpl implements ScmOrchestratorService {
  private SCMServiceGitClientImpl scmServiceGitClient;
  private JgitGitServiceImpl jgitGitService;

  @Override
  public CreateFileResponse createFile(ScmConnector scmConnector, GitFileDetails gitFileDetails) {
    return scmServiceGitClient.createFile(scmConnector, gitFileDetails);
  }

  @Override
  public UpdateFileResponse updateFile(ScmConnector scmConnector, GitFileDetails gitFileDetails) {
    return scmServiceGitClient.updateFile(scmConnector, gitFileDetails);
  }

  @Override
  public DeleteFileResponse deleteFile(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails) {
    return scmServiceGitClient.deleteFile(scmConnector, gitFilePathDetails);
  }

  @Override
  public FileContent getFileContent(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails) {
    return scmServiceGitClient.getFileContent(scmConnector, gitFilePathDetails);
  }

  @Override
  public FileBatchContentResponse getHarnessFilesOfBranch(ScmConnector scmConnector, String branch) {
    return scmServiceGitClient.getHarnessFilesOfBranch(scmConnector, branch);
  }

  @Override
  public FileBatchContentResponse listFiles(ScmConnector connector, List<String> filePaths, String branch) {
    return scmServiceGitClient.getHarnessFilesOfBranch(connector, branch);
  }

  @Override
  public FileContent getLatestFile(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails) {
    return scmServiceGitClient.getLatestFile(scmConnector, gitFilePathDetails);
  }

  @Override
  public IsLatestFileResponse isLatestFile(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, FileContent fileContent) {
    return scmServiceGitClient.isLatestFile(scmConnector, gitFilePathDetails, fileContent);
  }

  @Override
  public FileContent pushFile(ScmConnector scmConnector, GitFileDetails gitFileDetails) {
    return scmServiceGitClient.pushFile(scmConnector, gitFileDetails);
  }

  @Override
  public FindFilesInBranchResponse findFilesInBranch(ScmConnector scmConnector, String branch) {
    return scmServiceGitClient.findFilesInBranch(scmConnector, branch);
  }

  @Override
  public FindFilesInCommitResponse findFilesInCommit(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails) {
    return scmServiceGitClient.findFilesInCommit(scmConnector, gitFilePathDetails);
  }

  @Override
  public GetLatestCommitResponse getLatestCommit(ScmConnector scmConnector, String branch) {
    return scmServiceGitClient.getLatestCommit(scmConnector, branch);
  }

  @Override
  public ListBranchesResponse listBranches(ScmConnector scmConnector) {
    return scmServiceGitClient.listBranches(scmConnector);
  }

  @Override
  public ListCommitsResponse listCommits(ScmConnector scmConnector, String branch) {
    return scmServiceGitClient.listCommits(scmConnector, branch);
  }

  @Override
  public void createNewBranch(ScmConnector scmConnector, String branch, String defaultBranchName) {
    scmServiceGitClient.createNewBranch(scmConnector, branch, defaultBranchName);
  }
}
