package io.harness.istio.api.networking;

import static io.harness.rule.OwnerRule.TATHAGAT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDP)
public class IstioNetworkingApiFactoryTest extends CategoryTest {
  @Mock private V1Alpha3IstioApiNetworkingHandler v1Alpha3IstioApiNetworkingHandler;
  @Mock private V1Beta1IstioApiNetworkingHandler v1Beta1IstioApiNetworkingHandler;
  @InjectMocks IstioNetworkingApiFactory istioNetworkingApiFactory = new IstioNetworkingApiFactory();

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testObtainHandler() {
    assertThat(istioNetworkingApiFactory.obtainHandler("networking.istio.io/v1alpha3"))
        .isInstanceOf(V1Alpha3IstioApiNetworkingHandler.class);
    assertThat(istioNetworkingApiFactory.obtainHandler("networking.istio.io/v1beta1"))
        .isInstanceOf(V1Beta1IstioApiNetworkingHandler.class);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testObtainHandlerFail() {
    assertThatThrownBy(() -> istioNetworkingApiFactory.obtainHandler("random api"))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining("random api is not a valid api version");
  }
}