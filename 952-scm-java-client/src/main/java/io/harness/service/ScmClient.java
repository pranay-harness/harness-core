package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.UpdateFileResponse;

@OwnedBy(DX)
public interface ScmClient {
  // It is assumed that ScmConnector is a decrypted connector.
  CreateFileResponse createFile(ScmConnector scmConnector, GitFileDetails gitFileDetails);

  UpdateFileResponse updateFile(ScmConnector scmConnector, GitFileDetails gitFileDetails);

  DeleteFileResponse deleteFile(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails);

  FileContent getFileContent(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails);
}
