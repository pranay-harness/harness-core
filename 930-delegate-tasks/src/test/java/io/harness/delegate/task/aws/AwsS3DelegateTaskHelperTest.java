/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.aws;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsS3BucketResponse;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskParams;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskType;
import io.harness.encryption.SecretRefData;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;

import software.wings.service.impl.AwsApiHelperService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class AwsS3DelegateTaskHelperTest extends CategoryTest {
  @Mock private SecretDecryptionService secretDecryptionService;
  @Mock private AwsApiHelperService awsApiHelperService;
  @InjectMocks private AwsS3DelegateTaskHelper taskHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    on(taskHelper).set("awsNgConfigMapper", new AwsNgConfigMapper());
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldGetS3BucketsManualConfig() {
    AwsConnectorDTO awsConnectorDTO =
        AwsConnectorDTO.builder()
            .credential(
                AwsCredentialDTO.builder()
                    .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
                    .config(AwsManualConfigSpecDTO.builder()
                                .accessKey("test-access-key")
                                .secretKeyRef(SecretRefData.builder().decryptedValue("secret".toCharArray()).build())
                                .build())
                    .build())
            .build();
    AwsTaskParams awsTaskParams = AwsTaskParams.builder()
                                      .encryptionDetails(Collections.emptyList())
                                      .awsConnector(awsConnectorDTO)
                                      .awsTaskType(AwsTaskType.LIST_S3_BUCKETS)
                                      .region("test")
                                      .build();

    ArgumentCaptor<AwsInternalConfig> internalConfigArgumentCaptor = ArgumentCaptor.forClass(AwsInternalConfig.class);
    ArgumentCaptor<String> regionArgumentCaptor = ArgumentCaptor.forClass(String.class);
    List<String> buckets = Arrays.asList("bucket1", "bucket2");
    doReturn(null).when(secretDecryptionService).decrypt(any(), any());
    doReturn(buckets)
        .when(awsApiHelperService)
        .listS3Buckets(internalConfigArgumentCaptor.capture(), regionArgumentCaptor.capture());

    DelegateResponseData responseData = taskHelper.getS3Buckets(awsTaskParams);
    assertThat(responseData).isNotNull();
    assertThat(responseData).isInstanceOf(AwsS3BucketResponse.class);
    AwsS3BucketResponse awsS3BucketResponse = (AwsS3BucketResponse) responseData;
    assertThat(awsS3BucketResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(awsS3BucketResponse.getBuckets()).isNotEmpty();
    assertThat(awsS3BucketResponse.getBuckets()).containsKeys("bucket1", "bucket2");
    assertThat(awsS3BucketResponse.getBuckets()).containsValues("bucket1", "bucket2");

    AwsInternalConfig awsInternalConfig = internalConfigArgumentCaptor.getValue();
    assertThat(awsInternalConfig.getAccessKey()).isEqualTo("test-access-key".toCharArray());
    assertThat(awsInternalConfig.getSecretKey()).isEqualTo("secret".toCharArray());

    String region = regionArgumentCaptor.getValue();
    assertThat(region).isEqualTo("test");

    verify(secretDecryptionService, times(1)).decrypt(any(), any());
    verify(awsApiHelperService, times(1)).listS3Buckets(any(), any());
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldGetS3BucketsInheritFromDelegate() {
    AwsConnectorDTO awsConnectorDTO =
        AwsConnectorDTO.builder()
            .credential(AwsCredentialDTO.builder().awsCredentialType(AwsCredentialType.INHERIT_FROM_DELEGATE).build())
            .build();
    AwsTaskParams awsTaskParams = AwsTaskParams.builder()
                                      .encryptionDetails(Collections.emptyList())
                                      .awsConnector(awsConnectorDTO)
                                      .awsTaskType(AwsTaskType.LIST_S3_BUCKETS)
                                      .region("test")
                                      .build();

    ArgumentCaptor<AwsInternalConfig> internalConfigArgumentCaptor = ArgumentCaptor.forClass(AwsInternalConfig.class);
    ArgumentCaptor<String> regionArgumentCaptor = ArgumentCaptor.forClass(String.class);
    List<String> buckets = Arrays.asList("bucket1", "bucket2");
    doReturn(buckets)
        .when(awsApiHelperService)
        .listS3Buckets(internalConfigArgumentCaptor.capture(), regionArgumentCaptor.capture());

    DelegateResponseData responseData = taskHelper.getS3Buckets(awsTaskParams);
    assertThat(responseData).isNotNull();
    assertThat(responseData).isInstanceOf(AwsS3BucketResponse.class);
    AwsS3BucketResponse awsS3BucketResponse = (AwsS3BucketResponse) responseData;
    assertThat(awsS3BucketResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(awsS3BucketResponse.getBuckets()).isNotEmpty();
    assertThat(awsS3BucketResponse.getBuckets()).containsKeys("bucket1", "bucket2");
    assertThat(awsS3BucketResponse.getBuckets()).containsValues("bucket1", "bucket2");

    AwsInternalConfig awsInternalConfig = internalConfigArgumentCaptor.getValue();
    assertThat(awsInternalConfig.isUseEc2IamCredentials()).isTrue();

    String region = regionArgumentCaptor.getValue();
    assertThat(region).isEqualTo("test");

    verify(secretDecryptionService, times(0)).decrypt(any(), any());
    verify(awsApiHelperService, times(1)).listS3Buckets(any(), any());
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldGetNoS3BucketsIfEmptyList() {
    AwsConnectorDTO awsConnectorDTO =
        AwsConnectorDTO.builder()
            .credential(
                AwsCredentialDTO.builder()
                    .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
                    .config(AwsManualConfigSpecDTO.builder()
                                .accessKey("test-access-key")
                                .secretKeyRef(SecretRefData.builder().decryptedValue("secret".toCharArray()).build())
                                .build())
                    .build())
            .build();
    AwsTaskParams awsTaskParams = AwsTaskParams.builder()
                                      .encryptionDetails(Collections.emptyList())
                                      .awsConnector(awsConnectorDTO)
                                      .awsTaskType(AwsTaskType.LIST_S3_BUCKETS)
                                      .region("test")
                                      .build();

    ArgumentCaptor<AwsInternalConfig> internalConfigArgumentCaptor = ArgumentCaptor.forClass(AwsInternalConfig.class);
    ArgumentCaptor<String> regionArgumentCaptor = ArgumentCaptor.forClass(String.class);
    doReturn(null).when(secretDecryptionService).decrypt(any(), any());
    doReturn(Collections.emptyList())
        .when(awsApiHelperService)
        .listS3Buckets(internalConfigArgumentCaptor.capture(), regionArgumentCaptor.capture());

    DelegateResponseData responseData = taskHelper.getS3Buckets(awsTaskParams);
    assertThat(responseData).isNotNull();
    assertThat(responseData).isInstanceOf(AwsS3BucketResponse.class);
    AwsS3BucketResponse awsS3BucketResponse = (AwsS3BucketResponse) responseData;
    assertThat(awsS3BucketResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(awsS3BucketResponse.getBuckets()).isEmpty();

    AwsInternalConfig awsInternalConfig = internalConfigArgumentCaptor.getValue();
    assertThat(awsInternalConfig.getAccessKey()).isEqualTo("test-access-key".toCharArray());
    assertThat(awsInternalConfig.getSecretKey()).isEqualTo("secret".toCharArray());

    String region = regionArgumentCaptor.getValue();
    assertThat(region).isEqualTo("test");

    verify(secretDecryptionService, times(1)).decrypt(any(), any());
    verify(awsApiHelperService, times(1)).listS3Buckets(any(), any());
  }

  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldGetNoS3BucketsIfNull() {
    AwsConnectorDTO awsConnectorDTO =
        AwsConnectorDTO.builder()
            .credential(
                AwsCredentialDTO.builder()
                    .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
                    .config(AwsManualConfigSpecDTO.builder()
                                .accessKey("test-access-key")
                                .secretKeyRef(SecretRefData.builder().decryptedValue("secret".toCharArray()).build())
                                .build())
                    .build())
            .build();
    AwsTaskParams awsTaskParams = AwsTaskParams.builder()
                                      .encryptionDetails(Collections.emptyList())
                                      .awsConnector(awsConnectorDTO)
                                      .awsTaskType(AwsTaskType.LIST_S3_BUCKETS)
                                      .region("test")
                                      .build();

    ArgumentCaptor<AwsInternalConfig> internalConfigArgumentCaptor = ArgumentCaptor.forClass(AwsInternalConfig.class);
    ArgumentCaptor<String> regionArgumentCaptor = ArgumentCaptor.forClass(String.class);
    doReturn(null).when(secretDecryptionService).decrypt(any(), any());
    doReturn(null)
        .when(awsApiHelperService)
        .listS3Buckets(internalConfigArgumentCaptor.capture(), regionArgumentCaptor.capture());

    DelegateResponseData responseData = taskHelper.getS3Buckets(awsTaskParams);
    assertThat(responseData).isNotNull();
    assertThat(responseData).isInstanceOf(AwsS3BucketResponse.class);
    AwsS3BucketResponse awsS3BucketResponse = (AwsS3BucketResponse) responseData;
    assertThat(awsS3BucketResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(awsS3BucketResponse.getBuckets()).isEmpty();

    AwsInternalConfig awsInternalConfig = internalConfigArgumentCaptor.getValue();
    assertThat(awsInternalConfig.getAccessKey()).isEqualTo("test-access-key".toCharArray());
    assertThat(awsInternalConfig.getSecretKey()).isEqualTo("secret".toCharArray());

    String region = regionArgumentCaptor.getValue();
    assertThat(region).isEqualTo("test");

    verify(secretDecryptionService, times(1)).decrypt(any(), any());
    verify(awsApiHelperService, times(1)).listS3Buckets(any(), any());
  }
}
