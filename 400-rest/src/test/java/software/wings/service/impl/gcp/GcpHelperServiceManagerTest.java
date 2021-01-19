package software.wings.service.impl.gcp;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.gcp.helpers.GcpHelperService;
import io.harness.delegate.task.gcp.request.GcpValidationRequest;
import io.harness.delegate.task.gcp.response.GcpValidationTaskResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.service.impl.aws.model.AwsEc2ListInstancesResponse;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.EncryptionService;

import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class GcpHelperServiceManagerTest extends WingsBaseTest {
  @Mock private GcpHelperService gcpHelperService;
  @Mock private DelegateService delegateService;
  @Mock private EncryptionService encryptionService;
  @InjectMocks private GcpHelperServiceManager gcpHelperServiceManager;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void validateCredentialsServiceAccountFile() {
    final GcpConfig gcpConfig = GcpConfig.builder().serviceAccountKeyFileContent("secret".toCharArray()).build();
    gcpHelperServiceManager.validateCredential(gcpConfig, Collections.emptyList());
    verify(gcpHelperService)
        .getGkeContainerService(gcpConfig.getServiceAccountKeyFileContent(), gcpConfig.isUseDelegate());
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void validateCredentialDelegateSelector() throws InterruptedException {
    doReturn(
        GcpValidationTaskResponse.builder()
            .connectorValidationResult(ConnectorValidationResult.builder().status(ConnectivityStatus.SUCCESS).build())
            .build())
        .when(delegateService)
        .executeTask(any(DelegateTask.class));

    final GcpConfig gcpConfig =
        GcpConfig.builder().accountId(ACCOUNT_ID).useDelegate(true).delegateSelector("foo").build();

    gcpHelperServiceManager.validateCredential(gcpConfig, Collections.emptyList());

    ArgumentCaptor<DelegateTask> delegateTaskArgumentCaptor = ArgumentCaptor.forClass(DelegateTask.class);

    verify(delegateService, times(1)).executeTask(delegateTaskArgumentCaptor.capture());

    final DelegateTask delegateTask = delegateTaskArgumentCaptor.getValue();

    assertThat(delegateTask.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(delegateTask.getTags()).containsExactly("foo");
    assertThat(delegateTask.getData().getTimeout()).isEqualTo(TaskData.DEFAULT_SYNC_CALL_TIMEOUT);
    assertThat(delegateTask.getData().getParameters()[0])
        .isEqualTo(GcpValidationRequest.builder().delegateSelector("foo").build());
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void validateDelegateSuccessForSyncTask() {
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> gcpHelperServiceManager.validateDelegateSuccessForSyncTask(
                            ErrorNotifyResponseData.builder().errorMessage("err").build()))
        .withMessage("err");

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> gcpHelperServiceManager.validateDelegateSuccessForSyncTask(
                            RemoteMethodReturnValueData.builder()
                                .exception(new InvalidRequestException("err"))
                                .returnValue(new Object())
                                .build()))
        .withMessage("Invalid request: err");

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(()
                        -> gcpHelperServiceManager.validateDelegateSuccessForSyncTask(
                            AwsEc2ListInstancesResponse.builder().build()))
        .withMessage("Unknown response from delegate: [AwsEc2ListInstancesResponse]");

    gcpHelperServiceManager.validateDelegateSuccessForSyncTask(
        GcpValidationTaskResponse.builder()
            .connectorValidationResult(ConnectorValidationResult.builder().status(ConnectivityStatus.SUCCESS).build())
            .build());
  }
}
