/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.delegate.beans.connector.awsconnector.AwsCredentialType.MANUAL_CREDENTIALS;
import static io.harness.delegate.beans.connector.helm.HttpHelmAuthType.USER_PASSWORD;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthenticationDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmUsernamePasswordDTO;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.storeconfig.GcsHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.HttpHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.S3HelmStoreDelegateConfig;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.task.k8s.HelmChartManifestDelegateConfig;
import io.harness.encryption.SecretRefData;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class HelmValuesFetchTaskNGTest extends CategoryTest {
  @Mock private HelmTaskHelperBase helmTaskHelperBase;
  @Mock private SecretDecryptionService decryptionService;
  @Mock DecryptableEntity decryptableEntity;
  @Mock private ILogStreamingTaskClient logStreamingTaskClient;
  @Mock private LogCallback logCallback;

  @InjectMocks
  HelmValuesFetchTaskNG helmValuesFetchTaskNG =
      new HelmValuesFetchTaskNG(DelegateTaskPackage.builder()
                                    .delegateId("delegateid")
                                    .data(TaskData.builder().timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                                    .build(),
          logStreamingTaskClient, notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldExecuteHelmValueFetchFromS3() throws Exception {
    String valuesYaml = "values-file-content";
    AwsConnectorDTO connectorDTO =
        AwsConnectorDTO.builder()
            .credential(
                AwsCredentialDTO.builder()
                    .awsCredentialType(MANUAL_CREDENTIALS)
                    .config(AwsManualConfigSpecDTO.builder()
                                .accessKey("test-access-key")
                                .secretKeyRef(
                                    SecretRefData.builder().decryptedValue("test-secret-key".toCharArray()).build())
                                .build())
                    .build())
            .build();
    HelmChartManifestDelegateConfig manifestDelegateConfig =
        HelmChartManifestDelegateConfig.builder()
            .storeDelegateConfig(S3HelmStoreDelegateConfig.builder()
                                     .encryptedDataDetails(Collections.emptyList())
                                     .awsConnector(connectorDTO)
                                     .build())
            .build();

    doReturn(decryptableEntity).when(decryptionService).decrypt(any(), anyList());
    doReturn(valuesYaml)
        .when(helmTaskHelperBase)
        .fetchValuesYamlFromChart(eq(manifestDelegateConfig), eq(DEFAULT_ASYNC_CALL_TIMEOUT), any());

    HelmValuesFetchRequest request = HelmValuesFetchRequest.builder()
                                         .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                         .helmChartManifestDelegateConfig(manifestDelegateConfig)
                                         .accountId("test")
                                         .build();

    HelmValuesFetchResponse response = (HelmValuesFetchResponse) helmValuesFetchTaskNG.run(request);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response.getValuesFileContent()).isEqualTo(valuesYaml);
    assertThat(response.getUnitProgressData()).isNotNull();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldExecuteHelmValueFetchFromGcs() throws Exception {
    String valuesYaml = "values-file-content";
    GcpConnectorDTO connectorDTO =
        GcpConnectorDTO.builder()
            .credential(
                GcpConnectorCredentialDTO.builder()
                    .gcpCredentialType(GcpCredentialType.MANUAL_CREDENTIALS)
                    .config(GcpManualDetailsDTO.builder()
                                .secretKeyRef(SecretRefData.builder().decryptedValue("gcp-key".toCharArray()).build())
                                .build())
                    .build())
            .build();
    HelmChartManifestDelegateConfig manifestDelegateConfig =
        HelmChartManifestDelegateConfig.builder()
            .storeDelegateConfig(GcsHelmStoreDelegateConfig.builder()
                                     .encryptedDataDetails(Collections.emptyList())
                                     .gcpConnector(connectorDTO)
                                     .build())
            .build();

    doReturn(decryptableEntity).when(decryptionService).decrypt(any(), anyList());
    doReturn(valuesYaml)
        .when(helmTaskHelperBase)
        .fetchValuesYamlFromChart(eq(manifestDelegateConfig), eq(DEFAULT_ASYNC_CALL_TIMEOUT), any());

    HelmValuesFetchRequest request = HelmValuesFetchRequest.builder()
                                         .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                         .helmChartManifestDelegateConfig(manifestDelegateConfig)
                                         .accountId("test")
                                         .build();

    HelmValuesFetchResponse response = (HelmValuesFetchResponse) helmValuesFetchTaskNG.run(request);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response.getValuesFileContent()).isEqualTo(valuesYaml);
    assertThat(response.getUnitProgressData()).isNotNull();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldExecuteHelmValueFetchFromHttp() throws Exception {
    String valuesYaml = "values-file-content";
    HttpHelmConnectorDTO connectorDTO =
        HttpHelmConnectorDTO.builder()
            .auth(HttpHelmAuthenticationDTO.builder()
                      .authType(USER_PASSWORD)
                      .credentials(
                          HttpHelmUsernamePasswordDTO.builder()
                              .username("test")
                              .passwordRef(SecretRefData.builder().decryptedValue("password".toCharArray()).build())
                              .build())
                      .build())
            .build();
    HelmChartManifestDelegateConfig manifestDelegateConfig =
        HelmChartManifestDelegateConfig.builder()
            .storeDelegateConfig(HttpHelmStoreDelegateConfig.builder()
                                     .encryptedDataDetails(Collections.emptyList())
                                     .httpHelmConnector(connectorDTO)
                                     .build())
            .build();

    doReturn(decryptableEntity).when(decryptionService).decrypt(any(), anyList());
    doReturn(valuesYaml)
        .when(helmTaskHelperBase)
        .fetchValuesYamlFromChart(eq(manifestDelegateConfig), eq(DEFAULT_ASYNC_CALL_TIMEOUT), any());

    HelmValuesFetchRequest request = HelmValuesFetchRequest.builder()
                                         .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                         .helmChartManifestDelegateConfig(manifestDelegateConfig)
                                         .accountId("test")
                                         .build();

    HelmValuesFetchResponse response = (HelmValuesFetchResponse) helmValuesFetchTaskNG.run(request);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(response.getValuesFileContent()).isEqualTo(valuesYaml);
    assertThat(response.getUnitProgressData()).isNotNull();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldReturnErrorResponse() throws Exception {
    HelmValuesFetchTaskNG spyHelmValuesFetchTaskNG = spy(helmValuesFetchTaskNG);
    HttpHelmConnectorDTO connectorDTO =
        HttpHelmConnectorDTO.builder()
            .auth(HttpHelmAuthenticationDTO.builder()
                      .authType(USER_PASSWORD)
                      .credentials(
                          HttpHelmUsernamePasswordDTO.builder()
                              .username("test")
                              .passwordRef(SecretRefData.builder().decryptedValue("password".toCharArray()).build())
                              .build())
                      .build())
            .build();
    HelmChartManifestDelegateConfig manifestDelegateConfig =
        HelmChartManifestDelegateConfig.builder()
            .storeDelegateConfig(HttpHelmStoreDelegateConfig.builder()
                                     .encryptedDataDetails(Collections.emptyList())
                                     .httpHelmConnector(connectorDTO)
                                     .build())
            .build();

    doReturn(decryptableEntity).when(decryptionService).decrypt(any(), anyList());
    doThrow(new RuntimeException("Something went wrong"))
        .when(helmTaskHelperBase)
        .fetchValuesYamlFromChart(eq(manifestDelegateConfig), eq(DEFAULT_ASYNC_CALL_TIMEOUT), any());
    doReturn(logCallback).when(spyHelmValuesFetchTaskNG).getLogCallback(any());
    doNothing().when(logCallback).saveExecutionLog(anyString(), any(), any());

    HelmValuesFetchRequest request = HelmValuesFetchRequest.builder()
                                         .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                                         .helmChartManifestDelegateConfig(manifestDelegateConfig)
                                         .accountId("test")
                                         .build();

    assertThatThrownBy(() -> spyHelmValuesFetchTaskNG.run(request))
        .isInstanceOf(TaskNGDataException.class)
        .getRootCause()
        .hasMessageContaining("Something went wrong");
  }
}
