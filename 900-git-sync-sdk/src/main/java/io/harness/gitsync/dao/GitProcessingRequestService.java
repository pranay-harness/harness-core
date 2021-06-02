package io.harness.gitsync.dao;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.FileProcessingResponse;
import io.harness.gitsync.FileProcessingStatus;
import io.harness.gitsync.GitToHarnessProcessRequest;
import io.harness.gitsync.beans.GitProcessRequest;

import com.mongodb.client.result.UpdateResult;
import java.util.Map;

@OwnedBy(DX)
public interface GitProcessingRequestService {
  Map<String, FileProcessingResponse> upsert(GitToHarnessProcessRequest gitToHarnessProcessRequest);

  UpdateResult updateFileStatus(
      String commitId, String fileId, FileProcessingStatus status, String errorMsg, String accountId);

  GitProcessRequest get(String commitId, String accountId, String repo, String branch);
}
