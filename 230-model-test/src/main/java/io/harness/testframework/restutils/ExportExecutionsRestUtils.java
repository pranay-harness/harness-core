/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import static software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;

import io.harness.exception.InvalidRequestException;
import io.harness.execution.export.request.ExportExecutionsRequestLimitChecks;
import io.harness.execution.export.request.ExportExecutionsRequestSummary;
import io.harness.execution.export.request.ExportExecutionsUserParams;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import com.google.inject.Singleton;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.ws.rs.core.GenericType;
import org.apache.commons.io.IOUtils;

@Singleton
public class ExportExecutionsRestUtils {
  public static ExportExecutionsRequestLimitChecks getLimitChecks(
      String bearerToken, String accountId, String appId, String workflowId) {
    GenericType<RestResponse<ExportExecutionsRequestLimitChecks>> respType =
        new GenericType<RestResponse<ExportExecutionsRequestLimitChecks>>() {};

    RestResponse<ExportExecutionsRequestLimitChecks> resp =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .queryParam("appId", appId)
            .queryParam("search[0][field]", WorkflowExecutionKeys.workflowId)
            .queryParam("search[0][op]", "EQ")
            .queryParam("search[0][value]", workflowId)
            .get("/export-executions/limit-checks")
            .as(respType.getType());

    if (resp.getResource() == null) {
      throw new InvalidRequestException(String.valueOf(resp.getResponseMessages()));
    }
    return resp.getResource();
  }

  public static ExportExecutionsRequestSummary export(
      String bearerToken, String accountId, String appId, String workflowId, ExportExecutionsUserParams userParams) {
    GenericType<RestResponse<ExportExecutionsRequestSummary>> respType =
        new GenericType<RestResponse<ExportExecutionsRequestSummary>>() {};

    RestResponse<ExportExecutionsRequestSummary> resp =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .queryParam("appId", appId)
            .queryParam("search[0][field]", WorkflowExecutionKeys.workflowId)
            .queryParam("search[0][op]", "EQ")
            .queryParam("search[0][value]", workflowId)
            .body(userParams, ObjectMapperType.GSON)
            .contentType(ContentType.JSON)
            .post("/export-executions/export")
            .as(respType.getType());

    if (resp.getResource() == null) {
      throw new InvalidRequestException(String.valueOf(resp.getResponseMessages()));
    }
    return resp.getResource();
  }

  public static ExportExecutionsRequestSummary getStatus(String bearerToken, String accountId, String requestId) {
    GenericType<ExportExecutionsRequestSummary> respType = new GenericType<ExportExecutionsRequestSummary>() {};

    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .get("/export-executions/status/" + requestId)
        .as(respType.getType());
  }

  public static byte[] downloadFile(String bearerToken, String accountId, String requestId) {
    try (InputStream inputStream = Setup.portal()
                                       .auth()
                                       .oauth2(bearerToken)
                                       .queryParam("accountId", accountId)
                                       .get("/export-executions/download/" + requestId)
                                       .asInputStream();
         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      IOUtils.copy(inputStream, byteArrayOutputStream);
      return byteArrayOutputStream.toByteArray();
    } catch (Exception ex) {
      throw new InvalidRequestException("Could not download export executions file", ex);
    }
  }
}
