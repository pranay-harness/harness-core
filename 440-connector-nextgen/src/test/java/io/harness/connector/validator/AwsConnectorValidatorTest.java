package io.harness.connector.validator;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsInheritFromDelegateSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsValidateTaskResponse;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.service.DelegateGrpcClientWrapper;

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
                            .config(AwsInheritFromDelegateSpecDTO.builder().delegateSelector("delegate").build())
                            .build())
            .build();
    when(ngSecretService.getEncryptionDetails(any(), any())).thenReturn(null);
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(AwsValidateTaskResponse.builder().executionStatus(CommandExecutionStatus.SUCCESS).build());
    awsConnectorValidator.validate(awsConnectorDTO, "accountIdentifier", "orgIdentifier", "projectIdentifier");
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
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(AwsValidateTaskResponse.builder().executionStatus(CommandExecutionStatus.SUCCESS).build());
    awsConnectorValidator.validate(awsConnectorDTO, "accountIdentifier", "orgIdentifier", "projectIdentifier");
    verify(delegateGrpcClientWrapper, times(1)).executeSyncTask(any());
  }
}
