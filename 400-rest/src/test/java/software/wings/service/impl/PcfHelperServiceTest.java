package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.task.pcf.response.CfInstanceSyncResponse.builder;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.ANKIT;
import static io.harness.rule.OwnerRule.ANSHUL;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.pcf.response.CfCommandExecutionResponse;
import io.harness.delegate.task.pcf.response.CfInstanceSyncResponse;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.WingsBaseTest;
import software.wings.beans.PcfConfig;
import software.wings.service.intfc.DelegateService;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDP)
public class PcfHelperServiceTest extends WingsBaseTest {
  @Mock private DelegateService delegateService;
  @Mock private FeatureFlagService mockFeatureFlagService;
  @InjectMocks private PcfHelperService pcfHelperService;

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testValidate() throws Exception {
    PcfConfig pcfConfig = PcfConfig.builder().accountId(ACCOUNT_ID).build();
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    encryptedDataDetails.add(EncryptedDataDetail.builder().fieldName("password").build());

    when(delegateService.executeTask(any(DelegateTask.class))).thenReturn(CfCommandExecutionResponse.builder().build());
    doReturn(true).when(mockFeatureFlagService).isEnabled(any(), anyString());

    pcfHelperService.validate(pcfConfig, encryptedDataDetails);
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).executeTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask.getData().getParameters()).hasSize(2);
    List<EncryptedDataDetail> parameter = (List<EncryptedDataDetail>) (delegateTask.getData().getParameters()[1]);
    assertThat(parameter).isNotEmpty();
    assertThat(parameter.get(0).getFieldName()).isEqualTo("password");
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testInstancesCount() {
    List<String> instances = new ArrayList<>();
    instances.add("DummyInstanceId1");
    instances.add("DummyInstanceId2");

    CfInstanceSyncResponse cfInstanceSyncResponse = builder().instanceIndicesx(instances).build();
    CfCommandExecutionResponse response = CfCommandExecutionResponse.builder()
                                              .commandExecutionStatus(SUCCESS)
                                              .pcfCommandResponse(cfInstanceSyncResponse)
                                              .build();

    assertEquals(2, pcfHelperService.getInstanceCount(response));
  }
}
