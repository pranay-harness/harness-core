package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static software.wings.security.UserThreadLocal.userGuard;

import static org.assertj.core.api.Assertions.assertThat;

import software.wings.WingsBaseTest;
import software.wings.audit.AuditHeader;
import software.wings.beans.HttpMethod;
import software.wings.features.AuditTrailFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.FileService;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.IOException;
import org.apache.commons.lang3.RandomStringUtils;

public class AuditServiceTestHelper {
  @Inject protected AuditService auditService;

  public AuditHeader createAuditHeader(String appId) {
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
}
