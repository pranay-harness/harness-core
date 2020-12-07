package software.wings.service.impl.gcp;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GcpHttpTransportHelperServiceTest {
  private final GcpHttpTransportHelperService gcpHttpTransportHelperService = new GcpHttpTransportHelperService();

  @Test
  @Owner(developers = OwnerRule.AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnApacheHttpTransport() throws IOException, GeneralSecurityException {
    System.setProperty("http.proxyHost", "proxyHost");
    System.setProperty("http.proxyPort", "3218");
    HttpTransport httpTransport = gcpHttpTransportHelperService.checkIfUseProxyAndGetHttpTransport();
    assertThat(httpTransport).isInstanceOf(ApacheHttpTransport.class);
  }

  @Test
  @Owner(developers = OwnerRule.AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnNetHttpTransport() throws IOException, GeneralSecurityException {
    System.clearProperty("http.proxyHost");
    HttpTransport httpTransport = gcpHttpTransportHelperService.checkIfUseProxyAndGetHttpTransport();
    assertThat(httpTransport).isInstanceOf(NetHttpTransport.class);
  }
}
