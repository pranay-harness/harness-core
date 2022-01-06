/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.aws;

import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ACASIAN;
import static io.harness.rule.OwnerRule.ACHYUTH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AwsClient;
import io.harness.aws.AwsConfig;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectivityStatus;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsS3BucketResponse;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskParams;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskType;
import io.harness.delegate.beans.connector.awsconnector.AwsValidateTaskResponse;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import java.util.Collections;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(HarnessTeam.CDP)
public class AwsDelegateTaskTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private AwsClient awsClient;
  @Mock private AwsNgConfigMapper awsNgConfigMapper;
  @Mock private NGErrorHelper ngErrorHelper;
  @Mock private AwsS3DelegateTaskHelper awsS3DelegateTaskHelper;

  @InjectMocks
  private AwsDelegateTask task =
      new AwsDelegateTask(DelegateTaskPackage.builder().data(TaskData.builder().build()).build(), null, null, null);

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testRunWithObjectParams() {
    assertThatThrownBy(() -> task.run(new Object[10]))
        .hasMessage("Not implemented")
        .isInstanceOf(NotImplementedException.class);
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldSupportErrorFramework() {
    assertThat(task.isSupportingErrorFramework()).isTrue();
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldListS3Buckets() {
    AwsTaskParams awsTaskParams = AwsTaskParams.builder().awsTaskType(AwsTaskType.LIST_S3_BUCKETS).build();
    AwsS3BucketResponse response =
        AwsS3BucketResponse.builder().commandExecutionStatus(SUCCESS).buckets(Collections.emptyMap()).build();

    doReturn(response).when(awsS3DelegateTaskHelper).getS3Buckets(eq(awsTaskParams));

    DelegateResponseData result = task.run(awsTaskParams);
    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(AwsS3BucketResponse.class);
    assertThat(result).isEqualTo(response);

    verify(awsS3DelegateTaskHelper, times(1)).getS3Buckets(eq(awsTaskParams));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldHandleValidationTask() {
    AwsConnectorDTO awsConnectorDTO =
        AwsConnectorDTO.builder()
            .credential(AwsCredentialDTO.builder().awsCredentialType(AwsCredentialType.INHERIT_FROM_DELEGATE).build())
            .build();
    AwsTaskParams awsTaskParams = AwsTaskParams.builder()
                                      .awsConnector(awsConnectorDTO)
                                      .awsTaskType(AwsTaskType.VALIDATE)
                                      .encryptionDetails(Collections.emptyList())
                                      .build();

    AwsConfig awsConfig = AwsConfig.builder().build();
    doReturn(awsConfig).when(awsNgConfigMapper).mapAwsConfigWithDecryption(any(), any(), any());

    DelegateResponseData result = task.run(awsTaskParams);
    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(AwsValidateTaskResponse.class);
    AwsValidateTaskResponse awsValidateTaskResponse = (AwsValidateTaskResponse) result;
    assertThat(awsValidateTaskResponse.getConnectorValidationResult().getStatus())
        .isEqualTo(ConnectivityStatus.SUCCESS);
    assertThat(awsValidateTaskResponse.getConnectorValidationResult().getTestedAt()).isNotNull();

    verify(awsNgConfigMapper, times(1)).mapAwsConfigWithDecryption(any(), any(), any());
    verify(awsClient, times(1)).validateAwsAccountCredential(eq(awsConfig));
  }

  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testShouldHandleValidationTaskIRSA() {
    AwsConnectorDTO awsConnectorDTO =
        AwsConnectorDTO.builder()
            .credential(AwsCredentialDTO.builder().awsCredentialType(AwsCredentialType.IRSA).build())
            .build();
    AwsTaskParams awsTaskParams = AwsTaskParams.builder()
                                      .awsConnector(awsConnectorDTO)
                                      .awsTaskType(AwsTaskType.VALIDATE)
                                      .encryptionDetails(Collections.emptyList())
                                      .build();

    AwsConfig awsConfig = AwsConfig.builder().isIRSA(true).build();

    doReturn(awsConfig).when(awsNgConfigMapper).mapAwsConfigWithDecryption(any(), any(), any());

    DelegateResponseData result = task.run(awsTaskParams);
    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(AwsValidateTaskResponse.class);
    AwsValidateTaskResponse awsValidateTaskResponse = (AwsValidateTaskResponse) result;
    assertThat(awsValidateTaskResponse.getConnectorValidationResult().getStatus())
        .isEqualTo(ConnectivityStatus.SUCCESS);
    assertThat(awsValidateTaskResponse.getConnectorValidationResult().getTestedAt()).isNotNull();
    assertThat(awsConfig.isIRSA()).isEqualTo(true);

    verify(awsNgConfigMapper, times(1)).mapAwsConfigWithDecryption(any(), any(), any());
    verify(awsClient, times(1)).validateAwsAccountCredential(eq(awsConfig));
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void testShouldThrowExceptionIfTaskTypeNoSpecified() {
    AwsTaskParams awsTaskParams = AwsTaskParams.builder().awsTaskType(null).build();

    assertThatThrownBy(() -> task.run(awsTaskParams))
        .hasMessage("Task type not provided")
        .isInstanceOf(InvalidRequestException.class);
  }
}
