package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static software.wings.security.UserThreadLocal.userGuard;

import com.google.common.util.concurrent.FakeTimeLimiter;
import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.audit.AuditHeader;
import software.wings.beans.HttpMethod;
import software.wings.beans.User;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.FileService;

import java.io.IOException;

/**
 * Created by rishi on 5/19/16.
 */
public class AuditServiceTest extends WingsBaseTest {
  @Inject protected AuditService auditService;
  @Inject protected FileService fileService;

  protected String appId = generateUuid();

  public AuditHeader createAuditHeader() throws IOException {
    try (UserThreadLocal.Guard guard = userGuard(null)) {
      AuditHeader header =
          AuditHeader.Builder.anAuditHeader()
              .withAppId(appId)
              .withUrl("http://localhost:9090/wings/catalogs")
              .withResourcePath("catalogs")
              .withRequestMethod(HttpMethod.GET)
              .withHeaderString(
                  "Cache-Control=;no-cache,Accept=;*/*,Connection=;keep-alive,User-Agent=;Mozilla/5.0 (Macintosh; "
                  + "Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 "
                  + "Safari/537.36,Host=;localhost:9090,"
                  + "Postman-Token=;bdd7280e-bfac-b0f1-9603-c7b0e55a74af,"
                  + "Accept-Encoding=;"
                  + "gzip, deflate, sdch,Accept-Language=;en-US,en;q=0.8,Content-Type=;application/json")
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
  }

  @Before
  public void setupMocks() {
    on(auditService).set("timeLimiter", new FakeTimeLimiter());
  }

  @Test
  public void shouldCreate() throws Exception {
    createAuditHeader();
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

    PageResponse<AuditHeader> res =
        auditService.list(aPageRequest().withOffset("1").withLimit("2").addFilter("appId", Operator.EQ, appId).build());

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
  public void shouldUpdateUser() throws Exception {
    AuditHeader header = createAuditHeader();
    assertThat(header).isNotNull();
    assertThat(header.getRemoteUser()).isNull();
    User user = User.Builder.anUser().withUuid(generateUuid()).withName("abc").build();
    auditService.updateUser(header, user);
    AuditHeader header2 = auditService.read(header.getAppId(), header.getUuid());
    assertThat(header2).isNotNull();
    assertThat(header2.getRemoteUser()).isEqualTo(user);
  }

  @Test
  public void shouldDeleteAuditRecords() throws Exception {
    createAuditHeader();
    createAuditHeader();
    createAuditHeader();
    createAuditHeader();

    auditService.deleteAuditRecords(0);
    assertThat(auditService.list(aPageRequest().addFilter(ArtifactStream.APP_ID_KEY, EQ, appId).build())).hasSize(0);
  }

  @Test
  public void shouldNotDeleteAuditRecordsWithInRetentionTime() throws Exception {
    createAuditHeader();
    createAuditHeader();
    createAuditHeader();
    createAuditHeader();

    auditService.deleteAuditRecords(1 * 24 * 60 * 60 * 1000);
    assertThat(auditService.list(aPageRequest().addFilter(ArtifactStream.APP_ID_KEY, EQ, appId).build())).hasSize(4);
  }
}
