package io.harness.pcf.cfsdk;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.TMACARI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.rule.Owner;

import java.time.Duration;
import java.util.Optional;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@OwnedBy(HarnessTeam.CDP)
public class ConnectionContextProviderTest extends CategoryTest {
  @InjectMocks @Spy private ConnectionContextProvider connectionContextProvider;

  @Before
  public void setupMocks() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetConnectionContext() throws PivotalClientApiException {
    CfRequestConfig cfRequestConfig = CfRequestConfig.builder().endpointUrl("test").timeOutIntervalInMins(10).build();

    ConnectionContext connectionContext = connectionContextProvider.getConnectionContext(cfRequestConfig);

    assertThat(connectionContext).isInstanceOf(DefaultConnectionContext.class);
    Optional<Duration> connectTimeout = ((DefaultConnectionContext) connectionContext).getConnectTimeout();
    assertThat(connectTimeout.isPresent()).isTrue();
    assertThat(connectTimeout.get().getSeconds()).isEqualTo(300);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testIgnoringConnectionContextCache() throws PivotalClientApiException {
    CfRequestConfig cfRequestConfig =
        CfRequestConfig.builder().endpointUrl("test").ignorePcfConnectionContextCache(true).build();

    ConnectionContext firstConnectionContext = connectionContextProvider.getConnectionContext(cfRequestConfig);
    ConnectionContext secondConnectionContext = connectionContextProvider.getConnectionContext(cfRequestConfig);

    assertThat(firstConnectionContext).isNotSameAs(secondConnectionContext);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testNotIgnoringConnectionContextCache() throws PivotalClientApiException {
    CfRequestConfig cfRequestConfig =
        CfRequestConfig.builder().endpointUrl("test").ignorePcfConnectionContextCache(false).build();

    ConnectionContext firstConnectionContext = connectionContextProvider.getConnectionContext(cfRequestConfig);
    ConnectionContext secondConnectionContext = connectionContextProvider.getConnectionContext(cfRequestConfig);

    assertThat(firstConnectionContext).isSameAs(secondConnectionContext);
  }
}
