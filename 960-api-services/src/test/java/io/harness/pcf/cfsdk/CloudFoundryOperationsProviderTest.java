package io.harness.pcf.cfsdk;

import static io.harness.rule.OwnerRule.ANSHUL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@OwnedBy(HarnessTeam.CDP)
public class CloudFoundryOperationsProviderTest extends CategoryTest {
  @Mock private ConnectionContextProvider connectionContextProvider;
  @Mock private CloudFoundryClientProvider cloudFoundryClientProvider;

  @InjectMocks @Spy private CloudFoundryOperationsProvider cloudFoundryOperationsProvider;

  @Before
  public void setupMocks() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetCloudFoundryOperationsWrapper() throws PivotalClientApiException {
    CfRequestConfig cfRequestConfig = getCfRequestConfig();
    cfRequestConfig.setUserName("username");
    cfRequestConfig.setPassword("password");
    cfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    CloudFoundryOperationsWrapper cloudFoundryOperationsWrapper =
        cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(cfRequestConfig);

    assertThat(cloudFoundryOperationsWrapper).isNotNull();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetCFOperationsWrapperForConnectionContextException() {
    CfRequestConfig cfRequestConfig = getCfRequestConfig();

    try {
      cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(cfRequestConfig);
      fail("Should not reach here.");
    } catch (PivotalClientApiException e) {
      assertThat(e.getMessage()).isEqualTo("Exception while creating CloudFoundryOperations: NullPointerException");
    }
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testGetCFOperationsWrapperForTokenProviderException() {
    CfRequestConfig cfRequestConfig = getCfRequestConfig();
    cfRequestConfig.setUserName("username");
    cfRequestConfig.setEndpointUrl("api.run.pivotal.io");

    try {
      cloudFoundryOperationsProvider.getCloudFoundryOperationsWrapper(cfRequestConfig);
      fail("Should not reach here.");
    } catch (PivotalClientApiException e) {
      assertThat(e.getMessage())
          .isEqualTo("Exception while creating CloudFoundryOperations: NullPointerException: password");
    }
  }

  private CfRequestConfig getCfRequestConfig() {
    return CfRequestConfig.builder().timeOutIntervalInMins(1).orgName("org").applicationName("app").build();
  }
}
