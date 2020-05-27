package io.harness.execution.export.request;

import io.harness.CategoryTest;
import io.harness.execution.export.request.ExportExecutionsRequest.Status;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RequestTestUtils extends CategoryTest {
  public final String ACCOUNT_ID = "aid";
  public final String REQUEST_ID = "rid";
  public final String FILE_ID = "fid";
  public final int TOTAL_EXECUTIONS = 3;
  public final long CREATED_AT = System.currentTimeMillis();
  public final long EXPIRES_AT = System.currentTimeMillis() + 10 * 60 * 60 * 1000;
  public final String ERROR_MESSAGE = "err_msg";

  public ExportExecutionsRequest prepareExportExecutionsRequest() {
    return prepareExportExecutionsRequest(Status.QUEUED);
  }

  public ExportExecutionsRequest prepareExportExecutionsRequest(Status status) {
    return ExportExecutionsRequest.builder()
        .accountId(ACCOUNT_ID)
        .uuid(REQUEST_ID)
        .outputFormat(ExportExecutionsRequest.OutputFormat.JSON)
        .query(ExportExecutionsRequestQuery.builder().build())
        .status(status)
        .totalExecutions(TOTAL_EXECUTIONS)
        .expiresAt(EXPIRES_AT)
        .fileId(FILE_ID)
        .errorMessage(ERROR_MESSAGE)
        .createdAt(CREATED_AT)
        .nextIteration(1L)
        .nextCleanupIteration(2L)
        .build();
  }
}
