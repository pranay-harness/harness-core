package io.harness.cvng.connectiontask;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.NEMANJA;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectionTaskParams;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;

import java.util.Collections;

public class AppDynamicsTestConnectionDelegateTaskTest extends WingsBaseTest {
  @Mock AppdynamicsDelegateService appdynamicsDelegateService;
  @Mock SecretDecryptionService secretDecryptionService;

  String passwordRef = "passwordRef";
  SecretRefData passwordSecretRef = SecretRefData.builder().identifier(passwordRef).scope(Scope.ACCOUNT).build();

  AppDynamicsConnectorDTO appDynamicsConnectorDTO = AppDynamicsConnectorDTO.builder()
                                                        .username("username")
                                                        .controllerUrl("url")
                                                        .accountname("accountName")
                                                        .accountId("accountId")
                                                        .passwordRef(passwordSecretRef)
                                                        .build();

  @InjectMocks
  AppDynamicsTestConnectionDelegateTask appDynamicsTestConnectionDelegateTask =
      (AppDynamicsTestConnectionDelegateTask)
          TaskType.APPDYNAMICS_NG_CONFIGURATION_VALIDATE_TASK.getDelegateRunnableTask(
              DelegateTaskPackage.builder()
                  .delegateId("delegateId")
                  .data((TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                            .parameters(new Object[] {AppDynamicsConnectionTaskParams.builder()
                                                          .appDynamicsConnectorDTO(appDynamicsConnectorDTO)
                                                          .encryptionDetails(Collections.emptyList())
                                                          .build()})
                            .build())
                  .build(),
              notifyResponseData -> {}, () -> true);

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void run() {
    when(secretDecryptionService.decrypt(any(), anyList())).thenReturn(appDynamicsConnectorDTO);
    appDynamicsTestConnectionDelegateTask.run();
    verify(appdynamicsDelegateService, times(1)).validateConfig(any(AppDynamicsConnectorDTO.class), any());
  }
}