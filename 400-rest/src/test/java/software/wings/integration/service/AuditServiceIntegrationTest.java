/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.FileBucket.AUDITS;
import static io.harness.rule.OwnerRule.ADWAIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.PageRequest;
import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.service.AuditServiceTestHelper;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.FileService;

import com.google.inject.Inject;
import java.io.ByteArrayInputStream;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

// this test fails intermittently
public class AuditServiceIntegrationTest extends WingsBaseTest {
  @Inject private AuditServiceTestHelper auditServiceTestHelper;
  @Inject protected AuditService auditService;
  @Inject protected FileService fileService;

  @Test
  @Owner(developers = ADWAIT)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldCreateRequestPayload() throws Exception {
    AuditHeader header = auditServiceTestHelper.createAuditHeader(generateUuid());
    assertThat(header.getRequestTime()).isNull();
    assertThat(header.getRequestPayloadUuid()).isNull();
    byte[] httpBody = "TESTTESTTESTTESTTESTTESTTESTTESTTESTTEST".getBytes();
    String fileId = auditService.create(header, RequestType.REQUEST, new ByteArrayInputStream(httpBody));

    header.setResponseStatusCode(200);
    header.setResponseTime(System.currentTimeMillis());
    AuditHeader header2 = auditService.read(header.getAppId(), header.getUuid());
    assertThat(header2).isNotNull();
    assertThat(header2.getRequestPayloadUuid()).isNotNull();
    assertThat(header2.getRequestPayloadUuid()).isEqualTo(fileId);
  }

  /**
   * Should finalize.
   *
   * @throws Exception the exception
   */
  @Test
  @Owner(developers = ADWAIT)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldFinalize() throws Exception {
    AuditHeader header = auditServiceTestHelper.createAuditHeader(generateUuid());
    assertThat(header).isNotNull();
    assertThat(header.getRemoteUser()).isNull();
    byte[] httpBody = "TESTTESTTESTTESTTESTTESTTESTTESTTESTTEST".getBytes();
    header.setResponseTime(System.currentTimeMillis());
    header.setResponseStatusCode(200);
    auditService.finalize(header, httpBody);
    AuditHeader header2 = auditService.read(header.getAppId(), header.getUuid());
    assertThat(header2).isNotNull();
    assertThat(header2.getResponsePayloadUuid()).isNotNull();
    assertThat(header2.getResponseStatusCode()).isEqualTo(200);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldDeleteAuditRecordsRequestFiles() throws Exception {
    AuditHeader header = auditServiceTestHelper.createAuditHeader(generateUuid());
    assertThat(header.getRequestTime()).isNull();
    assertThat(header.getRequestPayloadUuid()).isNull();
    byte[] httpBody = "TESTTESTTESTTESTTESTTESTTESTTESTTESTTEST".getBytes();
    String requestFileId = auditService.create(header, RequestType.REQUEST, new ByteArrayInputStream(httpBody));
    String responseFileId = auditService.create(header, RequestType.RESPONSE, new ByteArrayInputStream(httpBody));

    header.setResponseStatusCode(200);
    header.setResponseTime(System.currentTimeMillis());
    AuditHeader header2 = auditService.read(header.getAppId(), header.getUuid());
    assertThat(header2).isNotNull();
    assertThat(header2.getRequestPayloadUuid()).isNotNull();
    assertThat(header2.getRequestPayloadUuid()).isEqualTo(requestFileId);
    assertThat(header2.getResponsePayloadUuid()).isNotNull();
    assertThat(header2.getResponsePayloadUuid()).isEqualTo(responseFileId);

    auditService.deleteAuditRecords(0);
    assertThat(auditService.list(new PageRequest<>())).hasSize(0);
    assertThat(fileService.getAllFileIds(header2.getRequestPayloadUuid(), AUDITS)).hasSize(0);
    assertThat(fileService.getAllFileIds(header2.getResponsePayloadUuid(), AUDITS)).hasSize(0);
  }
}
