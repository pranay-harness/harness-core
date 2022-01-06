/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.request;

import io.harness.execution.export.request.ExportExecutionsRequest.Status;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RequestTestUtils {
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
