package io.harness.cdng.service.steps;

import static io.harness.rule.OwnerRule.ADWAIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.ManifestConstants;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.service.beans.ServiceYaml;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ServiceStepTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @InjectMocks ServiceStep serviceStep;

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCreateServiceOutcome() throws IOException {
    K8sManifest k8Manifest =
        K8sManifest.builder()
            .identifier("m1")
            .storeConfigWrapper(
                StoreConfigWrapper.builder()
                    .storeConfig(GitStore.builder()
                                     .paths(ParameterField.createValueField(Collections.singletonList("path")))
                                     .connectorRef(ParameterField.createValueField("g1"))
                                     .gitFetchType(FetchType.BRANCH)
                                     .branch(ParameterField.createValueField("master"))
                                     .build())
                    .build())
            .build();

    K8sManifestOutcome k8sManifestOutcome = K8sManifestOutcome.builder()
                                                .identifier(k8Manifest.getIdentifier())
                                                .storeConfig(k8Manifest.getStoreConfig())
                                                .build();

    K8sManifest k8Manifest1 =
        K8sManifest.builder()
            .identifier("m2")
            .storeConfigWrapper(
                StoreConfigWrapper.builder()
                    .storeConfig(GitStore.builder()
                                     .paths(ParameterField.createValueField(Collections.singletonList("path1")))
                                     .connectorRef(ParameterField.createValueField("g1"))
                                     .gitFetchType(FetchType.BRANCH)
                                     .branch(ParameterField.createValueField("master"))
                                     .build())
                    .build())
            .build();

    K8sManifestOutcome k8sManifestOutcome1 = K8sManifestOutcome.builder()
                                                 .identifier(k8Manifest1.getIdentifier())
                                                 .storeConfig(k8Manifest1.getStoreConfig())
                                                 .build();

    ManifestsOutcome manifestsOutcome =
        ManifestsOutcome.builder().manifestOutcomeList(Arrays.asList(k8sManifestOutcome, k8sManifestOutcome1)).build();
    StepOutcome stepOutcome = StepOutcome.builder().outcome(manifestsOutcome).name(ManifestConstants.MANIFESTS).build();

    ServiceYaml entity = ServiceYaml.builder().identifier("s1").name("s1").build();
    ServiceOutcome serviceOutcome = serviceStep.createServiceOutcome(
        ServiceConfig.builder()
            .service(entity)
            .serviceDefinition(ServiceDefinition.builder().serviceSpec(KubernetesServiceSpec.builder().build()).build())
            .build(),
        Collections.singletonList(stepOutcome), 123);

    assertThat(serviceOutcome.getManifestResults()).isNotEmpty();
    assertThat(serviceOutcome.getManifestResults().keySet().size()).isEqualTo(2);
    assertThat(serviceOutcome.getManifestResults().get(k8Manifest.getIdentifier())).isEqualTo(k8sManifestOutcome);
    assertThat(serviceOutcome.getManifestResults().get(k8Manifest1.getIdentifier())).isEqualTo(k8sManifestOutcome1);
  }
}
