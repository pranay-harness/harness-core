package io.harness.cdng.k8s;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.executionplan.CDStepDependencyKey;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.stepsdependency.utils.CDStepDependencyUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.executionplan.stepsdependency.StepDependencyResolverContext;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.executionplan.stepsdependency.bean.KeyAwareStepDependencySpec;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class K8sRollingStepTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock StepDependencyService stepDependencyService;
  @InjectMocks private K8sRollingStep k8sRollingStep;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testStartChainLink() {
    StepDependencySpec serviceSpec = KeyAwareStepDependencySpec.builder().key("SERVICE").build();
    StepDependencySpec infraSpec = KeyAwareStepDependencySpec.builder().key("INFRA").build();
    Map<String, StepDependencySpec> stepDependencySpecs = new HashMap<String, StepDependencySpec>() {
      {
        put(CDStepDependencyKey.SERVICE.name(), serviceSpec);
        put(CDStepDependencyKey.INFRASTRUCTURE.name(), infraSpec);
      }
    };
    K8sRollingStepInfo k8sRollingStepInfo =
        K8sRollingStepInfo.infoBuilder().stepDependencySpecs(stepDependencySpecs).build();

    Ambiance ambiance = Ambiance.newBuilder().build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    StepDependencyResolverContext resolverContext =
        CDStepDependencyUtils.getStepDependencyResolverContext(stepInputPackage, k8sRollingStepInfo, ambiance);

    doReturn(Optional.empty()).when(stepDependencyService).resolve(serviceSpec, resolverContext);
    assertThatThrownBy(() -> k8sRollingStep.startChainLink(ambiance, k8sRollingStepInfo, stepInputPackage))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining("Service Dependency is not available");

    ServiceOutcome serviceOutcome = ServiceOutcome.builder().build();
    doReturn(Optional.of(serviceOutcome)).when(stepDependencyService).resolve(serviceSpec, resolverContext);

    doReturn(Optional.empty()).when(stepDependencyService).resolve(infraSpec, resolverContext);
    assertThatThrownBy(() -> k8sRollingStep.startChainLink(ambiance, k8sRollingStepInfo, stepInputPackage))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining("Infrastructure Dependency is not available");
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

    List<ValuesManifest> aggregatedValuesManifests = k8sRollingStep.getAggregatedValuesManifests(serviceManifests);
    assertThat(aggregatedValuesManifests).hasSize(1);
    assertThat(aggregatedValuesManifests.get(0)).isEqualTo(valuesManifest);
  }
}
