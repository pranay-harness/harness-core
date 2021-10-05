package io.harness.istio.api.networking;

import static io.harness.rule.OwnerRule.TATHAGAT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.rule.Owner;

import io.kubernetes.client.openapi.ApiException;
import java.util.ArrayList;
import java.util.List;
import me.snowdrop.istio.api.networking.v1alpha3.Subset;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class V1Alpha3IstioApiNetworkingHandlerTest extends CategoryTest {
  V1Alpha3IstioApiNetworkingHandler v1Alpha3IstioApiNetworkingHandler = new V1Alpha3IstioApiNetworkingHandler();

  @Before
  public void setup() throws ApiException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testGenerateSubsetsForDestinationRule() {
    List<String> subsetNames = new ArrayList<>();
    subsetNames.add(HarnessLabelValues.trackCanary);
    subsetNames.add(HarnessLabelValues.trackStable);
    subsetNames.add(HarnessLabelValues.colorBlue);
    subsetNames.add(HarnessLabelValues.colorGreen);

    final List<Subset> result = v1Alpha3IstioApiNetworkingHandler.generateSubsetsForDestinationRule(subsetNames);

    assertThat(result.size()).isEqualTo(4);
  }
}