package software.wings.delegatetasks.validation;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.PcfConfig;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRouteUpdateRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.request.PcfInstanceSyncRequest;

import java.util.function.Consumer;

public class PcfCommandValidationTest extends CategoryTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidationWithNullEncryptionType() {
    Consumer consumer = mock(Consumer.class);
    PCFCommandValidation pcfCommandValidation =
        new PCFCommandValidation("", DelegateTask.builder().data(TaskData.builder().build()).build(), consumer);

    PcfCommandRequest request = PcfCommandSetupRequest.builder().useAppAutoscalar(true).build();
    assertThat(pcfCommandValidation.needToCheckAppAutoscalarPluginInstall(request)).isTrue();
    request.setUseAppAutoscalar(false);
    assertThat(pcfCommandValidation.needToCheckAppAutoscalarPluginInstall(request)).isFalse();

    request = PcfCommandDeployRequest.builder().useAppAutoscalar(true).build();
    assertThat(pcfCommandValidation.needToCheckAppAutoscalarPluginInstall(request)).isTrue();
    request.setUseAppAutoscalar(false);
    assertThat(pcfCommandValidation.needToCheckAppAutoscalarPluginInstall(request)).isFalse();

    request = PcfCommandRouteUpdateRequest.builder().useAppAutoscalar(true).build();
    assertThat(pcfCommandValidation.needToCheckAppAutoscalarPluginInstall(request)).isTrue();
    request.setUseAppAutoscalar(false);
    assertThat(pcfCommandValidation.needToCheckAppAutoscalarPluginInstall(request)).isFalse();

    request = PcfCommandRollbackRequest.builder().useAppAutoscalar(true).build();
    assertThat(pcfCommandValidation.needToCheckAppAutoscalarPluginInstall(request)).isTrue();
    request.setUseAppAutoscalar(false);
    assertThat(pcfCommandValidation.needToCheckAppAutoscalarPluginInstall(request)).isFalse();

    request = PcfInstanceSyncRequest.builder().build();
    assertThat(pcfCommandValidation.needToCheckAppAutoscalarPluginInstall(request)).isFalse();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testNeedToCheckAppAutoscalarPluginInstall() {
    Consumer consumer = mock(Consumer.class);
    PCFCommandValidation pcfCommandValidation =
        new PCFCommandValidation("", DelegateTask.builder().data(TaskData.builder().build()).build(), consumer);

    PcfConfig pcfConfig = PcfConfig.builder().endpointUrl("url").username("user").build();
    PcfCommandRequest request = PcfCommandSetupRequest.builder().pcfConfig(pcfConfig).build();
    String criteria = pcfCommandValidation.getCriteria(request);
    assertThat(criteria).isEqualTo("Pcf:url/user");

    request = PcfCommandSetupRequest.builder().pcfConfig(pcfConfig).useCLIForPcfAppCreation(true).build();
    criteria = pcfCommandValidation.getCriteria(request);
    assertThat(criteria).isEqualTo("Pcf:url/user_cf_cli");

    request = PcfCommandSetupRequest.builder()
                  .pcfConfig(pcfConfig)
                  .useCLIForPcfAppCreation(true)
                  .useAppAutoscalar(true)
                  .build();
    criteria = pcfCommandValidation.getCriteria(request);
    assertThat(criteria).isEqualTo("Pcf:url/user_cf_cli_cf_appautoscalar");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPcfCliValidationRequired() {
    Consumer consumer = mock(Consumer.class);
    PCFCommandValidation pcfCommandValidation =
        new PCFCommandValidation("", DelegateTask.builder().data(TaskData.builder().build()).build(), consumer);

    PcfConfig pcfConfig = PcfConfig.builder().endpointUrl("url").username("user").build();
    PcfCommandSetupRequest request = PcfCommandSetupRequest.builder().pcfConfig(pcfConfig).build();
    assertThat(pcfCommandValidation.pcfCliValidationRequired(request)).isFalse();

    request.setUseAppAutoscalar(true);
    assertThat(pcfCommandValidation.pcfCliValidationRequired(request)).isTrue();

    request.setUseCLIForPcfAppCreation(true);
    assertThat(pcfCommandValidation.pcfCliValidationRequired(request)).isTrue();
  }
}
