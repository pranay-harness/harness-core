package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.beans.HttpMethod;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.User;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.rules.RealMongo;
import software.wings.service.intfc.AuditService;
import software.wings.utils.JsonUtils;

import java.io.ByteArrayInputStream;
import javax.inject.Inject;

/**
 * Created by rishi on 5/19/16.
 */
public class AuditServiceTest extends WingsBaseTest {
  @Inject private AuditService auditService;

  @Inject private JsonUtils jsonUtils;

  private String appId = UUIDGenerator.getUuid();

  /**
   * Should create.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldCreate() throws Exception {
    createAuditHeader();
  }

  private AuditHeader createAuditHeader() {
    AuditHeader header =
        AuditHeader.Builder.anAuditHeader()
            .withAppId(appId)
            .withUrl("http://localhost:9090/wings/catalogs")
            .withResourcePath("catalogs")
            .withRequestMethod(HttpMethod.GET)
            .withHeaderString(
                "Cache-Control=;no-cache,Accept=;*/*,Connection=;keep-alive,User-Agent=;Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36,Host=;localhost:9090,Postman-Token=;bdd7280e-bfac-b0f1-9603-c7b0e55a74af,Accept-Encoding=;gzip, deflate, sdch,Accept-Language=;en-US,en;q=0.8,Content-Type=;application/json")
            .withRemoteHostName("0:0:0:0:0:0:0:1")
            .withRemoteHostPort(555555)
            .withRemoteIpAddress("0:0:0:0:0:0:0:1")
            .withLocalHostName("Rishis-MacBook-Pro.local")
            .withLocalIpAddress("192.168.0.110")
            .build();
    auditService.create(header);
    AuditHeader header2 = auditService.read(header.getAppId(), header.getUuid());
    assertThat(header2).isNotNull();
    assertThat(header2.getUuid()).isNotNull();
    assertThat(header2).isEqualToComparingFieldByField(header);
    return header;
  }

  @Test
  @RealMongo
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
    assertThat(header2.getRequestPayloadUuid()).isEqualTo(fileId);
    assertThat(header2.getRequestPayloadUuid()).isNotNull();
  }

  /**
   * Should list.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldList() throws Exception {
    createAuditHeader();
    createAuditHeader();
    createAuditHeader();
    createAuditHeader();

    PageResponse<AuditHeader> res = auditService.list(
        PageRequest.Builder.aPageRequest()
            .withOffset("1")
            .withLimit("2")
            .addFilter(SearchFilter.Builder.aSearchFilter().withField("appId", Operator.EQ, appId).build())
            .build());

    assertThat(res).isNotNull();
    assertThat(res.size()).isEqualTo(2);
    assertThat(res.getTotal()).isEqualTo(4);
    assertThat(res.getPageSize()).isEqualTo(2);
    assertThat(res.getStart()).isEqualTo(1);
    assertThat(res.getResponse()).isNotNull();
    assertThat(res.getResponse().size()).isEqualTo(2);
  }

  /**
   * Should update user.
   *
   * @throws Exception the exception
   */
  @Test
  @RealMongo
  public void shouldUpdateUser() throws Exception {
    AuditHeader header = createAuditHeader();
    assertThat(header).isNotNull();
    assertThat(header.getRemoteUser()).isNull();
    User user = User.Builder.anUser().withUuid(UUIDGenerator.getUuid()).withName("abc").build();
    auditService.updateUser(header, user);
    AuditHeader header2 = auditService.read(header.getAppId(), header.getUuid());
    assertThat(header2).isNotNull();
    assertThat(header2.getRemoteUser()).isEqualTo(user);
  }

  /**
   * Should finalize.
   *
   * @throws Exception the exception
   */
  @Test
  @RealMongo
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
}
