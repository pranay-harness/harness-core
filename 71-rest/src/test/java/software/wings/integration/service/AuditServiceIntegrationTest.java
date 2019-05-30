package software.wings.integration.service;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.service.intfc.FileService.FileBucket.AUDITS;

import io.harness.beans.PageRequest;
import io.harness.category.element.IntegrationTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.service.AuditServiceTest;

import java.io.ByteArrayInputStream;

/**
 * Created by rishi on 5/19/16.
 */
// this test fails intermittently
public class AuditServiceIntegrationTest extends AuditServiceTest {
  @Test
  @Owner(emails = ADWAIT)
  @Category(IntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldCreateRequestPayload() throws Exception {
    AuditHeader header = createAuditHeader();
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
  @Owner(emails = ADWAIT)
  @Category(IntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldFinalize() throws Exception {
    AuditHeader header = createAuditHeader();
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
  @Owner(emails = ADWAIT)
  @Category(IntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldDeleteAuditRecordsRequestFiles() throws Exception {
    AuditHeader header = createAuditHeader();
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
