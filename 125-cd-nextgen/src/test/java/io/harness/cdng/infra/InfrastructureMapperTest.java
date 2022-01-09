/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.infra;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sGcpInfrastructureOutcome;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.infra.yaml.K8sGcpInfrastructure;
import io.harness.cdng.service.steps.ServiceStepOutcome;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.steps.environment.EnvironmentOutcome;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class InfrastructureMapperTest extends CategoryTest {
  private final EnvironmentOutcome environment =
      EnvironmentOutcome.builder().identifier("env").type(EnvironmentType.Production).build();
  private final ServiceStepOutcome serviceOutcome = ServiceStepOutcome.builder().identifier("service").build();

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
            .environment(environment)
            .infrastructureKey("11f6673d11711af46238bf33972cb99a4a869244")
            .build();

    InfrastructureOutcome infrastructureOutcome =
        InfrastructureMapper.toOutcome(k8SDirectInfrastructure, environment, serviceOutcome);
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

    assertThatThrownBy(() -> InfrastructureMapper.toOutcome(emptyReleaseName, environment, serviceOutcome))
        .isInstanceOf(InvalidArgumentsException.class);

    K8SDirectInfrastructure emptyNamespace = K8SDirectInfrastructure.builder()
                                                 .connectorRef(ParameterField.createValueField("connectorId"))
                                                 .namespace(ParameterField.createValueField(""))
                                                 .releaseName(ParameterField.createValueField("releaseName"))
                                                 .build();

    assertThatThrownBy(() -> InfrastructureMapper.toOutcome(emptyNamespace, environment, serviceOutcome))
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

    K8sGcpInfrastructureOutcome k8sGcpInfrastructureOutcome =
        K8sGcpInfrastructureOutcome.builder()
            .connectorRef("connectorId")
            .namespace("namespace")
            .releaseName("release")
            .cluster("cluster")
            .environment(environment)
            .infrastructureKey("54874007d7082ff0ab54cd51865954f5e78c5c88")
            .build();

    InfrastructureOutcome infrastructureOutcome =
        InfrastructureMapper.toOutcome(k8SGcpInfrastructure, environment, serviceOutcome);
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
    assertThatThrownBy(() -> InfrastructureMapper.toOutcome(emptyNamespace, environment, serviceOutcome))
        .isInstanceOf(InvalidArgumentsException.class);

    K8sGcpInfrastructure emptyReleaseName = K8sGcpInfrastructure.builder()
                                                .connectorRef(ParameterField.createValueField("connectorId"))
                                                .namespace(ParameterField.createValueField("namespace"))
                                                .releaseName(ParameterField.createValueField(""))
                                                .cluster(ParameterField.createValueField("cluster"))
                                                .build();
    assertThatThrownBy(() -> InfrastructureMapper.toOutcome(emptyReleaseName, environment, serviceOutcome))
        .isInstanceOf(InvalidArgumentsException.class);

    K8sGcpInfrastructure emptyClusterName = K8sGcpInfrastructure.builder()
                                                .connectorRef(ParameterField.createValueField("connectorId"))
                                                .namespace(ParameterField.createValueField("namespace"))
                                                .releaseName(ParameterField.createValueField("release"))
                                                .cluster(ParameterField.createValueField(""))
                                                .build();
    assertThatThrownBy(() -> InfrastructureMapper.toOutcome(emptyClusterName, environment, serviceOutcome))
        .isInstanceOf(InvalidArgumentsException.class);
  }
}
