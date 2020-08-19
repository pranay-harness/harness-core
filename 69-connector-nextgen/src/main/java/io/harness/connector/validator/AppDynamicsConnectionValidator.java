package io.harness.connector.validator;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ManagerDelegateServiceDriver;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectionTaskParams;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectionTaskResponse;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class AppDynamicsConnectionValidator implements ConnectionValidator<AppDynamicsConnectorDTO> {
  private final ManagerDelegateServiceDriver managerDelegateServiceDriver;
  private final SecretManagerClientService ngSecretService;
  @Override
  public ConnectorValidationResult validate(AppDynamicsConnectorDTO appDynamicsConnectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier) {
    Map<String, String> setupAbstractions = ImmutableMap.of("accountId", accountIdentifier);

    NGAccess basicNGAccessObject = BaseNGAccess.builder()
                                       .accountIdentifier(accountIdentifier)
                                       .orgIdentifier(orgIdentifier)
                                       .projectIdentifier(projectIdentifier)
                                       .build();

    List<EncryptedDataDetail> encryptedDataDetailList =
        ngSecretService.getEncryptionDetails(basicNGAccessObject, appDynamicsConnectorDTO);

    TaskData taskData = TaskData.builder()
                            .async(false)
                            .taskType("APPDYNAMICS_NG_CONFIGURATION_VALIDATE_TASK")
                            .parameters(new Object[] {AppDynamicsConnectionTaskParams.builder()
                                                          .appDynamicsConnectorDTO(appDynamicsConnectorDTO)
                                                          .encryptionDetails(encryptedDataDetailList)
                                                          .build()})
                            .timeout(TimeUnit.MINUTES.toMillis(1))
                            .build();

    AppDynamicsConnectionTaskResponse responseData =
        managerDelegateServiceDriver.sendTask(accountIdentifier, setupAbstractions, taskData);

    return ConnectorValidationResult.builder()
        .valid(responseData.isValid())
        .errorMessage(responseData.getErrorMessage())
        .build();
  }
}
