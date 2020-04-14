package software.wings.service.impl;

import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.PcfConfig;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.service.intfc.DelegateService;

import java.util.ArrayList;
import java.util.List;

public class PcfHelperServiceTest extends WingsBaseTest {
  @Mock private DelegateService delegateService;
  @InjectMocks private PcfHelperService pcfHelperService;

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testValidate() throws Exception {
    PcfConfig pcfConfig = PcfConfig.builder().accountId(ACCOUNT_ID).build();
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    encryptedDataDetails.add(EncryptedDataDetail.builder().fieldName("password").build());

    when(delegateService.executeTask(any(DelegateTask.class)))
        .thenReturn(PcfCommandExecutionResponse.builder().build());

    pcfHelperService.validate(pcfConfig, encryptedDataDetails);
    ArgumentCaptor<DelegateTask> captor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).executeTask(captor.capture());
    DelegateTask delegateTask = captor.getValue();
    assertThat(delegateTask.getData().getParameters()).hasSize(2);
    List<EncryptedDataDetail> parameter = (List<EncryptedDataDetail>) (delegateTask.getData().getParameters()[1]);
    assertThat(parameter).isNotEmpty();
    assertThat(parameter.get(0).getFieldName()).isEqualTo("password");
  }
}
