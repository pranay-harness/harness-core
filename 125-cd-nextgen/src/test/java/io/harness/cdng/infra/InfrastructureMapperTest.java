package io.harness.cdng.infra;

import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sGcpInfrastructureOutcome;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.infra.yaml.K8sGcpInfrastructure;
import io.harness.exception.InvalidArgumentsException;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InfrastructureMapperTest extends CategoryTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testToOutcome() {
    K8SDirectInfrastructure k8SDirectInfrastructure = K8SDirectInfrastructure.builder()
                                                          .connectorRef(ParameterField.createValueField("connectorId"))
                                                          .namespace(ParameterField.createValueField("namespace"))
                                                          .releaseName(ParameterField.createValueField("release"))
                                                          .build();

    K8sDirectInfrastructureOutcome k8sDirectInfrastructureOutcome =
        K8sDirectInfrastructureOutcome.builder()
            .connectorRef("connectorId")
            .namespace("namespace")
            .releaseName("release")
            .environment(EnvironmentOutcome.builder().build())
            .build();

    InfrastructureOutcome infrastructureOutcome =
        InfrastructureMapper.toOutcome(k8SDirectInfrastructure, EnvironmentOutcome.builder().build());
    assertThat(infrastructureOutcome).isEqualTo(k8sDirectInfrastructureOutcome);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testToOutcomeEmptyValues() {
    K8SDirectInfrastructure emptyReleaseName = K8SDirectInfrastructure.builder()
                                                   .connectorRef(ParameterField.createValueField("connectorId"))
                                                   .namespace(ParameterField.createValueField("namespace"))
                                                   .releaseName(ParameterField.createValueField(""))
                                                   .build();

    assertThatThrownBy(() -> InfrastructureMapper.toOutcome(emptyReleaseName, EnvironmentOutcome.builder().build()))
        .isInstanceOf(InvalidArgumentsException.class);

    K8SDirectInfrastructure emptyNamespace = K8SDirectInfrastructure.builder()
                                                 .connectorRef(ParameterField.createValueField("connectorId"))
                                                 .namespace(ParameterField.createValueField(""))
                                                 .releaseName(ParameterField.createValueField("releaseName"))
                                                 .build();

    assertThatThrownBy(() -> InfrastructureMapper.toOutcome(emptyNamespace, EnvironmentOutcome.builder().build()))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testK8sGcpInfraMapper() {
    K8sGcpInfrastructure k8SGcpInfrastructure = K8sGcpInfrastructure.builder()
                                                    .connectorRef(ParameterField.createValueField("connectorId"))
                                                    .namespace(ParameterField.createValueField("namespace"))
                                                    .releaseName(ParameterField.createValueField("release"))
                                                    .cluster(ParameterField.createValueField("cluster"))
                                                    .build();

    K8sGcpInfrastructureOutcome k8sGcpInfrastructureOutcome = K8sGcpInfrastructureOutcome.builder()
                                                                  .connectorRef("connectorId")
                                                                  .namespace("namespace")
                                                                  .releaseName("release")
                                                                  .cluster("cluster")
                                                                  .environment(EnvironmentOutcome.builder().build())
                                                                  .build();

    InfrastructureOutcome infrastructureOutcome =
        InfrastructureMapper.toOutcome(k8SGcpInfrastructure, EnvironmentOutcome.builder().build());
    assertThat(infrastructureOutcome).isEqualTo(k8sGcpInfrastructureOutcome);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testK8sGcpInfraMapperEmptyValues() {
    K8sGcpInfrastructure emptyNamespace = K8sGcpInfrastructure.builder()
                                              .connectorRef(ParameterField.createValueField("connectorId"))
                                              .namespace(ParameterField.createValueField(""))
                                              .releaseName(ParameterField.createValueField("release"))
                                              .cluster(ParameterField.createValueField("cluster"))
                                              .build();
    assertThatThrownBy(() -> InfrastructureMapper.toOutcome(emptyNamespace, EnvironmentOutcome.builder().build()))
        .isInstanceOf(InvalidArgumentsException.class);

    K8sGcpInfrastructure emptyReleaseName = K8sGcpInfrastructure.builder()
                                                .connectorRef(ParameterField.createValueField("connectorId"))
                                                .namespace(ParameterField.createValueField("namespace"))
                                                .releaseName(ParameterField.createValueField(""))
                                                .cluster(ParameterField.createValueField("cluster"))
                                                .build();
    assertThatThrownBy(() -> InfrastructureMapper.toOutcome(emptyReleaseName, EnvironmentOutcome.builder().build()))
        .isInstanceOf(InvalidArgumentsException.class);
  }
}
