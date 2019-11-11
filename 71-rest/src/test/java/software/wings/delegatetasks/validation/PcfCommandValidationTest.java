package software.wings.delegatetasks.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRouteUpdateRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.request.PcfInstanceSyncRequest;

import java.util.function.Consumer;

public class PcfCommandValidationTest extends CategoryTest {
  @Test
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
}
