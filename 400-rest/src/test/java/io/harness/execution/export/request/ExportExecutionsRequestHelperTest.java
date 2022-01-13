/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.request;

import static io.harness.rule.OwnerRule.GARVIT;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.ExportExecutionsException;
import io.harness.execution.export.request.ExportExecutionsRequest.Status;
import io.harness.rule.Owner;

import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;

import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ExportExecutionsRequestHelperTest extends CategoryTest {
  @Mock private MainConfiguration mainConfiguration;

  @Inject @InjectMocks private ExportExecutionsRequestHelper exportExecutionsRequestHelper;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testPrepareSummary() {
    when(mainConfiguration.getApiUrl()).thenReturn("http://localhost:8080/");
    String expectedStatusLink = format("http://localhost:8080/api/export-executions/status/%s?accountId=%s",
        RequestTestUtils.REQUEST_ID, RequestTestUtils.ACCOUNT_ID);
    String expectedDownloadLink = format("http://localhost:8080/api/export-executions/download/%s?accountId=%s",
        RequestTestUtils.REQUEST_ID, RequestTestUtils.ACCOUNT_ID);

    ExportExecutionsRequestSummary summary =
        exportExecutionsRequestHelper.prepareSummary(RequestTestUtils.prepareExportExecutionsRequest());
    assertThat(summary).isNotNull();
    assertThat(summary.getRequestId()).isEqualTo(RequestTestUtils.REQUEST_ID);
    assertThat(summary.getStatus()).isEqualTo(Status.QUEUED);
    assertThat(summary.getTotalExecutions()).isEqualTo(RequestTestUtils.TOTAL_EXECUTIONS);
    assertThat(summary.getTriggeredAt().toInstant().toEpochMilli()).isEqualTo(RequestTestUtils.CREATED_AT);
    assertThat(summary.getStatusLink()).isEqualTo(expectedStatusLink);
    assertThat(summary.getDownloadLink()).isEqualTo(expectedDownloadLink);
    assertThat(summary.getExpiresAt().toInstant().toEpochMilli()).isEqualTo(RequestTestUtils.EXPIRES_AT);
    assertThat(summary.getErrorMessage()).isNull();

    summary =
        exportExecutionsRequestHelper.prepareSummary(RequestTestUtils.prepareExportExecutionsRequest(Status.READY));
    assertThat(summary).isNotNull();
    assertThat(summary.getRequestId()).isEqualTo(RequestTestUtils.REQUEST_ID);
    assertThat(summary.getStatus()).isEqualTo(Status.READY);
    assertThat(summary.getTotalExecutions()).isEqualTo(RequestTestUtils.TOTAL_EXECUTIONS);
    assertThat(summary.getTriggeredAt().toInstant().toEpochMilli()).isEqualTo(RequestTestUtils.CREATED_AT);
    assertThat(summary.getStatusLink()).isEqualTo(expectedStatusLink);
    assertThat(summary.getDownloadLink()).isEqualTo(expectedDownloadLink);
    assertThat(summary.getExpiresAt().toInstant().toEpochMilli()).isEqualTo(RequestTestUtils.EXPIRES_AT);
    assertThat(summary.getErrorMessage()).isNull();

    summary =
        exportExecutionsRequestHelper.prepareSummary(RequestTestUtils.prepareExportExecutionsRequest(Status.FAILED));
    assertThat(summary).isNotNull();
    assertThat(summary.getRequestId()).isEqualTo(RequestTestUtils.REQUEST_ID);
    assertThat(summary.getStatus()).isEqualTo(Status.FAILED);
    assertThat(summary.getTotalExecutions()).isEqualTo(RequestTestUtils.TOTAL_EXECUTIONS);
    assertThat(summary.getTriggeredAt().toInstant().toEpochMilli()).isEqualTo(RequestTestUtils.CREATED_AT);
    assertThat(summary.getStatusLink()).isNull();
    assertThat(summary.getDownloadLink()).isNull();
    assertThat(summary.getExpiresAt()).isNull();
    assertThat(summary.getErrorMessage()).isEqualTo(RequestTestUtils.ERROR_MESSAGE);

    summary =
        exportExecutionsRequestHelper.prepareSummary(RequestTestUtils.prepareExportExecutionsRequest(Status.EXPIRED));
    assertThat(summary).isNotNull();
    assertThat(summary.getRequestId()).isEqualTo(RequestTestUtils.REQUEST_ID);
    assertThat(summary.getStatus()).isEqualTo(Status.EXPIRED);
    assertThat(summary.getTotalExecutions()).isEqualTo(RequestTestUtils.TOTAL_EXECUTIONS);
    assertThat(summary.getTriggeredAt().toInstant().toEpochMilli()).isEqualTo(RequestTestUtils.CREATED_AT);
    assertThat(summary.getStatusLink()).isNull();
    assertThat(summary.getDownloadLink()).isNull();
    assertThat(summary.getExpiresAt()).isNull();
    assertThat(summary.getErrorMessage()).isNull();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testPrepareLink() {
    when(mainConfiguration.getApiUrl()).thenReturn("http://localhost:8080/api");
    assertThat(exportExecutionsRequestHelper.prepareLink("aid", "rid", "a/b/c"))
        .isEqualTo("http://localhost:8080/api/export-executions/a/b/c/rid?accountId=aid");

    when(mainConfiguration.getApiUrl()).thenReturn("http://localhost:8080/api/");
    assertThat(exportExecutionsRequestHelper.prepareLink("aid", "rid", "a/b/c"))
        .isEqualTo("http://localhost:8080/api/export-executions/a/b/c/rid?accountId=aid");

    when(mainConfiguration.getApiUrl()).thenReturn("http://localhost:8080");
    assertThat(exportExecutionsRequestHelper.prepareLink("aid", "rid", "a/b/c"))
        .isEqualTo("http://localhost:8080/api/export-executions/a/b/c/rid?accountId=aid");

    when(mainConfiguration.getApiUrl()).thenReturn("http://localhost:8080/");
    assertThat(exportExecutionsRequestHelper.prepareLink("aid", "rid", "a/b/c"))
        .isEqualTo("http://localhost:8080/api/export-executions/a/b/c/rid?accountId=aid");

    when(mainConfiguration.getApiUrl()).thenReturn(null);
    PortalConfig portalConfig = new PortalConfig();
    portalConfig.setUrl("http://localhost:8080");
    when(mainConfiguration.getPortal()).thenReturn(portalConfig);
    assertThat(exportExecutionsRequestHelper.prepareLink("aid", "rid", "a/b/c"))
        .isEqualTo("http://localhost:8080/api/export-executions/a/b/c/rid?accountId=aid");

    when(mainConfiguration.getApiUrl()).thenReturn("%%bad-url%%");
    assertThatThrownBy(() -> exportExecutionsRequestHelper.prepareLink("aid", "rid", "a/b/c"))
        .isInstanceOf(ExportExecutionsException.class);
  }
}
