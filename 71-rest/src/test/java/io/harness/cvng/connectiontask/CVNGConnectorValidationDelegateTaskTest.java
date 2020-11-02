package io.harness.cvng.connectiontask;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;

import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.category.element.UnitTests;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.cvconnector.CVConnectorTaskParams;
import io.harness.delegate.beans.connector.cvconnector.CVConnectorTaskResponse;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;

public class CVNGConnectorValidationDelegateTaskTest extends WingsBaseTest {
  @Mock SecretDecryptionService secretDecryptionService;

  String passwordRef = "passwordRef";
  SecretRefData passwordSecretRef =
      SecretRefData.builder().identifier(passwordRef).decryptedValue("123".toCharArray()).scope(Scope.ACCOUNT).build();

  SplunkConnectorDTO splunkConnectorDTO = SplunkConnectorDTO.builder()
                                              .username("username")
                                              .splunkUrl("url")
                                              .accountId("accountId")
                                              .passwordRef(passwordSecretRef)
                                              .build();

  @InjectMocks
  CVNGConnectorValidationDelegateTask cvngConnectorValidateTaskDelegateRunnableTask =
      (CVNGConnectorValidationDelegateTask) TaskType.CVNG_CONNECTOR_VALIDATE_TASK.getDelegateRunnableTask(
          DelegateTaskPackage.builder()
              .delegateId("delegateId")
              .data((TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                        .parameters(new TaskParameters[] {CVConnectorTaskParams.builder()
                                                              .connectorConfigDTO(splunkConnectorDTO)
                                                              .encryptionDetails(Collections.emptyList())
                                                              .build()})
                        .build())
              .build(),
          notifyResponseData -> {}, () -> true);

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testRun() throws IllegalAccessException {
    when(secretDecryptionService.decrypt(any(), anyList())).thenReturn(splunkConnectorDTO);
    DataCollectionDSLService dataCollectionDSLService = mock(DataCollectionDSLService.class);
    Clock clock = Clock.fixed(Instant.parse("2020-04-22T10:02:06Z"), ZoneOffset.UTC);
    FieldUtils.writeField(
        cvngConnectorValidateTaskDelegateRunnableTask, "dataCollectionDSLService", dataCollectionDSLService, true);
    FieldUtils.writeField(cvngConnectorValidateTaskDelegateRunnableTask, "clock", clock, true);
    when(dataCollectionDSLService.execute(any(), any())).thenReturn("true");
    DelegateResponseData delegateResponseData =
        cvngConnectorValidateTaskDelegateRunnableTask.run(CVConnectorTaskParams.builder()
                                                              .connectorConfigDTO(splunkConnectorDTO)
                                                              .encryptionDetails(Collections.emptyList())
                                                              .build());
    CVConnectorTaskResponse cvConnectorTaskResponse = (CVConnectorTaskResponse) delegateResponseData;
    assertThat(cvConnectorTaskResponse).isEqualTo(CVConnectorTaskResponse.builder().valid(true).build());
  }
}