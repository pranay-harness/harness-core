/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.infra.steps;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.ACHYUTH;
import static io.harness.rule.OwnerRule.SAHIL;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.K8sDirectInfraMapping;
import io.harness.cdng.infra.beans.K8sGcpInfraMapping;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure.K8SDirectInfrastructureBuilder;
import io.harness.cdng.infra.yaml.K8sGcpInfrastructure;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.service.steps.ServiceStepOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.sdk.EntityValidityDetails;
import io.harness.logstreaming.ILogStreamingStepClient;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.yaml.ParameterField;
import io.harness.reflection.ReflectionUtils;
import io.harness.rule.Owner;
import io.harness.steps.OutputExpressionConstants;
import io.harness.steps.environment.EnvironmentOutcome;

import com.google.inject.name.Named;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(CDP)
public class InfrastructureStepTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock EnvironmentService environmentService;
  @Mock ConnectorService connectorService;
  @InjectMocks private InfrastructureStep infrastructureStep;

  @Mock LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Mock ILogStreamingStepClient iLogStreamingStepClient;
  @Mock ExecutionSweepingOutputService executionSweepingOutputService;
  @Mock OutcomeService outcomeService;
  @Mock K8sStepHelper k8sStepHelper;
  @Mock K8sInfraDelegateConfig k8sInfraDelegateConfig;

  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testValidateResource() {
    Ambiance ambiance = Ambiance.newBuilder().build();
    K8SDirectInfrastructureBuilder k8SDirectInfrastructureBuilder = K8SDirectInfrastructure.builder();

    infrastructureStep.validateResources(ambiance, k8SDirectInfrastructureBuilder.build());
  }

  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testExecSyncAfterRbac() {
    Ambiance ambiance = Ambiance.newBuilder().build();

    GcpConnectorDTO gcpConnectorServiceAccount =
        GcpConnectorDTO.builder()
            .credential(GcpConnectorCredentialDTO.builder()
                            .gcpCredentialType(GcpCredentialType.MANUAL_CREDENTIALS)
                            .config(GcpManualDetailsDTO.builder().build())
                            .build())
            .build();
    doReturn(Optional.of(ConnectorResponseDTO.builder()
                             .connector(ConnectorInfoDTO.builder().connectorConfig(gcpConnectorServiceAccount).build())
                             .entityValidityDetails(EntityValidityDetails.builder().valid(true).build())
                             .build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), eq("gcp-sa"));

    Infrastructure infrastructureSpec = K8sGcpInfrastructure.builder()
                                            .connectorRef(ParameterField.createValueField("account.gcp-sa"))
                                            .namespace(ParameterField.createValueField("namespace"))
                                            .releaseName(ParameterField.createValueField("releaseName"))
                                            .cluster(ParameterField.createValueField("cluster"))
                                            .build();

    when(logStreamingStepClientFactory.getLogStreamingStepClient(ambiance)).thenReturn(iLogStreamingStepClient);
    doNothing().when(iLogStreamingStepClient).openStream(any());

    when(executionSweepingOutputService.resolve(
             any(), eq(RefObjectUtils.getSweepingOutputRefObject(OutputExpressionConstants.ENVIRONMENT))))
        .thenReturn(EnvironmentOutcome.builder().build());
    when(outcomeService.resolve(any(), eq(RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.SERVICE))))
        .thenReturn(ServiceStepOutcome.builder().build());
    when(k8sStepHelper.getK8sInfraDelegateConfig(any(), eq(ambiance))).thenReturn(k8sInfraDelegateConfig);

    infrastructureStep.executeSyncAfterRbac(ambiance, infrastructureSpec, StepInputPackage.builder().build(), null);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testPrivelegaedAccessControlClient() throws NoSuchFieldException {
    assertThat(ReflectionUtils.getFieldByName(InfrastructureStep.class, "accessControlClient")
                   .getAnnotation(Named.class)
                   .value())
        .isEqualTo("PRIVILEGED");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testCreateInfraMappingObject() {
    String namespace = "namespace";
    String connector = "connector";

    Infrastructure infrastructureSpec = K8SDirectInfrastructure.builder()
                                            .connectorRef(ParameterField.createValueField(connector))
                                            .namespace(ParameterField.createValueField(namespace))
                                            .build();

    InfraMapping expectedInfraMapping =
        K8sDirectInfraMapping.builder().k8sConnector(connector).namespace(namespace).build();

    InfraMapping infraMapping = infrastructureStep.createInfraMappingObject(infrastructureSpec);
    assertThat(infraMapping).isEqualTo(expectedInfraMapping);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testCreateK8sGcpInfraMapping() {
    String namespace = "namespace";
    String connector = "connector";
    String cluster = "cluster";

    Infrastructure infrastructureSpec = K8sGcpInfrastructure.builder()
                                            .connectorRef(ParameterField.createValueField(connector))
                                            .namespace(ParameterField.createValueField(namespace))
                                            .cluster(ParameterField.createValueField(cluster))
                                            .build();

    InfraMapping expectedInfraMapping =
        K8sGcpInfraMapping.builder().gcpConnector(connector).namespace(namespace).cluster(cluster).build();

    InfraMapping infraMapping = infrastructureStep.createInfraMappingObject(infrastructureSpec);
    assertThat(infraMapping).isEqualTo(expectedInfraMapping);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testProcessEnvironment() {
    // TODO this test is not asserting anything.
    HashMap<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put(SetupAbstractionKeys.accountId, "accountId");
    setupAbstractions.put(SetupAbstractionKeys.projectIdentifier, "projectId");
    setupAbstractions.put(SetupAbstractionKeys.orgIdentifier, "orgId");
    Ambiance ambiance = Ambiance.newBuilder().putAllSetupAbstractions(setupAbstractions).build();

    EnvironmentYaml environmentYaml = EnvironmentYaml.builder()
                                          .identifier("test-id")
                                          .name("test-id")
                                          .type(EnvironmentType.PreProduction)
                                          .tags(Collections.emptyMap())
                                          .build();

    PipelineInfrastructure pipelineInfrastructure =
        PipelineInfrastructure.builder().environment(environmentYaml).build();

    Environment expectedEnv = Environment.builder()
                                  .identifier("test-id")
                                  .type(EnvironmentType.PreProduction)
                                  .tags(Collections.emptyList())
                                  .build();

    doReturn(expectedEnv).when(environmentService).upsert(expectedEnv);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testValidateInfrastructure() {
    assertThatThrownBy(() -> infrastructureStep.validateInfrastructure(null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Infrastructure definition can't be null or empty");

    K8SDirectInfrastructureBuilder k8SDirectInfrastructureBuilder = K8SDirectInfrastructure.builder();
    infrastructureStep.validateInfrastructure(k8SDirectInfrastructureBuilder.build());

    k8SDirectInfrastructureBuilder.connectorRef(ParameterField.createValueField("connector"));
    infrastructureStep.validateInfrastructure(k8SDirectInfrastructureBuilder.build());

    k8SDirectInfrastructureBuilder.connectorRef(new ParameterField<>(null, true, "expression1", null, true));
    assertThatThrownBy(() -> infrastructureStep.validateInfrastructure(k8SDirectInfrastructureBuilder.build()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Unresolved Expression : [expression1]");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testValidateConnector() {
    GcpConnectorDTO gcpConnectorServiceAccount =
        GcpConnectorDTO.builder()
            .credential(GcpConnectorCredentialDTO.builder()
                            .gcpCredentialType(GcpCredentialType.MANUAL_CREDENTIALS)
                            .config(GcpManualDetailsDTO.builder().build())
                            .build())
            .build();
    GcpConnectorDTO gcpConnectorInheritFromDelegate =
        GcpConnectorDTO.builder()
            .credential(
                GcpConnectorCredentialDTO.builder().gcpCredentialType(GcpCredentialType.INHERIT_FROM_DELEGATE).build())
            .build();
    doReturn(Optional.empty()).when(connectorService).get(anyString(), anyString(), anyString(), eq("missing"));
    doReturn(Optional.of(ConnectorResponseDTO.builder()
                             .entityValidityDetails(EntityValidityDetails.builder().valid(true).build())
                             .connector(ConnectorInfoDTO.builder().connectorConfig(gcpConnectorServiceAccount).build())
                             .build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), eq("gcp-sa"));
    doReturn(
        Optional.of(ConnectorResponseDTO.builder()
                        .entityValidityDetails(EntityValidityDetails.builder().valid(true).build())
                        .connector(ConnectorInfoDTO.builder().connectorConfig(gcpConnectorInheritFromDelegate).build())
                        .build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), eq("gcp-delegate"));

    assertConnectorValidationMessage(
        K8sGcpInfrastructure.builder().connectorRef(ParameterField.createValueField("account.missing")).build(),
        "Connector not found for identifier : [account.missing]");

    assertConnectorValidationMessage(
        K8sGcpInfrastructure.builder().connectorRef(ParameterField.createValueField("account.gcp-delegate")).build(),
        "Deployment using Google Kubernetes Engine infrastructure with inheriting credentials from delegate is not supported yet");

    assertThatCode(
        ()
            -> infrastructureStep.validateConnector(
                K8sGcpInfrastructure.builder().connectorRef(ParameterField.createValueField("account.gcp-sa")).build(),
                Ambiance.newBuilder().build()))
        .doesNotThrowAnyException();
  }

  private void assertConnectorValidationMessage(Infrastructure infrastructure, String message) {
    Ambiance ambiance = Ambiance.newBuilder().build();
    assertThatThrownBy(() -> infrastructureStep.validateConnector(infrastructure, ambiance))
        .hasMessageContaining(message);
  }
}
