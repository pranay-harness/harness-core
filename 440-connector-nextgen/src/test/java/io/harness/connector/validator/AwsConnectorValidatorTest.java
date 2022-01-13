/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.validator;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.helper.EncryptionHelper;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsInheritFromDelegateSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsValidateTaskResponse;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.service.DelegateGrpcClientWrapper;

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Slf4j
public class AwsConnectorValidatorTest extends CategoryTest {
  @Mock private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Mock private SecretManagerClientService ngSecretService;
  @Mock private EncryptionHelper encryptionHelper;
  @InjectMocks private AwsConnectorValidator awsConnectorValidator;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.ABHINAV)
  @Category(UnitTests.class)
  public void validateTestWithEc2Iam() {
    AwsConnectorDTO awsConnectorDTO =
        AwsConnectorDTO.builder()
            .credential(AwsCredentialDTO.builder()
                            .awsCredentialType(AwsCredentialType.INHERIT_FROM_DELEGATE)
                            .crossAccountAccess(CrossAccountAccessDTO.builder().build())
                            .config(AwsInheritFromDelegateSpecDTO.builder()
                                        .delegateSelectors(Collections.singleton("delegate"))
                                        .build())
                            .build())
            .build();
    when(ngSecretService.getEncryptionDetails(any(), any())).thenReturn(null);
    when(encryptionHelper.getEncryptionDetail(any(), any(), any(), any())).thenReturn(null);

    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(AwsValidateTaskResponse.builder()
                        .connectorValidationResult(
                            ConnectorValidationResult.builder().status(ConnectivityStatus.SUCCESS).build())
                        .build());
    awsConnectorValidator.validate(
        awsConnectorDTO, "accountIdentifier", "orgIdentifier", "projectIdentifier", "identifier");
    verify(delegateGrpcClientWrapper, times(1)).executeSyncTask(any());
  }

  @Test
  @Owner(developers = OwnerRule.ABHINAV)
  @Category(UnitTests.class)
  public void validateTestWithAccessKey() {
    SecretRefData passwordSecretRef =
        SecretRefData.builder().identifier("passwordRefIdentifier").scope(Scope.ACCOUNT).build();
    AwsConnectorDTO awsConnectorDTO =
        AwsConnectorDTO.builder()
            .credential(
                AwsCredentialDTO.builder()
                    .crossAccountAccess(CrossAccountAccessDTO.builder().build())
                    .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
                    .config(
                        AwsManualConfigSpecDTO.builder().secretKeyRef(passwordSecretRef).accessKey("accessKey").build())
                    .build())
            .build();
    when(ngSecretService.getEncryptionDetails(any(), any())).thenReturn(null);
    when(encryptionHelper.getEncryptionDetail(any(), any(), any(), any())).thenReturn(null);
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(AwsValidateTaskResponse.builder()
                        .connectorValidationResult(
                            ConnectorValidationResult.builder().status(ConnectivityStatus.SUCCESS).build())
                        .build());
    awsConnectorValidator.validate(
        awsConnectorDTO, "accountIdentifier", "orgIdentifier", "projectIdentifier", "identifier");
    verify(delegateGrpcClientWrapper, times(1)).executeSyncTask(any());
  }
}
