package io.harness.cdng.k8s;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.cdng.pipeline.K8sRollingStepInfo;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.exception.InvalidRequestException;
import io.harness.references.OutcomeRefObject;
import io.harness.rule.Owner;
import io.harness.state.io.K8sRollingStepParameters;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class K8sRollingStepTest extends WingsBaseTest {
  private K8sRollingStep k8sRollingStepSpy = new K8sRollingStep();
  @Mock OutcomeService outcomeService;
  @InjectMocks private K8sRollingStep k8sRollingStep;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testStartChainLink() {
    K8sRollingStepInfo k8sRollingStepInfo =
        K8sRollingStepInfo.builder().k8sRolling(K8sRollingStepParameters.builder().build()).build();

    Ambiance ambiance = Ambiance.builder().build();
    OutcomeRefObject service = OutcomeRefObject.builder().name("service").build();

    doReturn(null).when(outcomeService).resolve(ambiance, service);

    assertThatThrownBy(() -> k8sRollingStep.startChainLink(ambiance, k8sRollingStepInfo, Collections.emptyList()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Services step need to run before k8s rolling step");

    ServiceOutcome serviceOutcome = ServiceOutcome.builder().build();
    doReturn(serviceOutcome).when(outcomeService).resolve(ambiance, service);

    OutcomeRefObject infrastructureRefObject = OutcomeRefObject.builder().name("infrastructure").build();

    doReturn(null).when(outcomeService).resolve(ambiance, infrastructureRefObject);
    assertThatThrownBy(() -> k8sRollingStep.startChainLink(ambiance, k8sRollingStepInfo, Collections.emptyList()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Infrastructure step need to run before k8s rolling step");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetK8sManifests() {
    assertThatThrownBy(() -> k8sRollingStep.getK8sManifest(Collections.emptyList()))
        .hasMessageContaining("K8s Manifests are mandatory for k8s Rolling step");

    K8sManifest k8sManifest = K8sManifest.builder().build();
    ValuesManifest valuesManifest = ValuesManifest.builder().build();
    List<ManifestAttributes> serviceManifests = new ArrayList<>();
    serviceManifests.add(k8sManifest);
    serviceManifests.add(valuesManifest);

    K8sManifest actualK8sManifest = k8sRollingStep.getK8sManifest(serviceManifests);
    assertThat(actualK8sManifest).isEqualTo(k8sManifest);

    K8sManifest anotherK8sManifest = K8sManifest.builder().build();
    serviceManifests.add(anotherK8sManifest);

    assertThatThrownBy(() -> k8sRollingStep.getK8sManifest(serviceManifests))
        .hasMessageContaining("There can be only a single K8s manifest");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetAggregatedValuesManifests() {
    K8sManifest k8sManifest = K8sManifest.builder().build();
    ValuesManifest valuesManifest = ValuesManifest.builder().build();
    List<ManifestAttributes> serviceManifests = new ArrayList<>();
    serviceManifests.add(k8sManifest);
    serviceManifests.add(valuesManifest);

    List<ValuesManifest> aggregatedValuesManifests =
        k8sRollingStep.getAggregatedValuesManifests(serviceManifests, Collections.emptyList());
    assertThat(aggregatedValuesManifests).hasSize(1);
    assertThat(aggregatedValuesManifests.get(0)).isEqualTo(valuesManifest);

    // With Overrides
    ValuesManifest overridesValues1 = ValuesManifest.builder().build();
    ValuesManifest overridesValues2 = ValuesManifest.builder().build();
    List<ManifestAttributes> overridesValuesManifests = Arrays.asList(overridesValues1, overridesValues2);

    aggregatedValuesManifests = k8sRollingStep.getAggregatedValuesManifests(serviceManifests, overridesValuesManifests);
    assertThat(aggregatedValuesManifests).hasSize(3);
    assertThat(aggregatedValuesManifests.get(0)).isEqualTo(valuesManifest);
    assertThat(aggregatedValuesManifests.get(1)).isEqualTo(overridesValues1);
    assertThat(aggregatedValuesManifests.get(2)).isEqualTo(overridesValues2);
  }
}