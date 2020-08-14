package software.wings.delegatetasks.validation;

import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.PcfConfig;
import software.wings.helpers.ext.pcf.request.PcfCommandDeployRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRollbackRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandRouteUpdateRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandSetupRequest;
import software.wings.helpers.ext.pcf.request.PcfInstanceSyncRequest;
import software.wings.service.intfc.security.EncryptionService;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class PcfCommandValidationTest extends CategoryTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidationWithNullEncryptionType() {
    Consumer consumer = mock(Consumer.class);
    PCFCommandValidation pcfCommandValidation =
        new PCFCommandValidation("", DelegateTaskPackage.builder().data(TaskData.builder().build()).build(), consumer);

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
        new PCFCommandValidation("", DelegateTaskPackage.builder().data(TaskData.builder().build()).build(), consumer);

    PcfConfig pcfConfig = PcfConfig.builder().endpointUrl("url").username("user".toCharArray()).build();
    PcfCommandRequest request = PcfCommandSetupRequest.builder().pcfConfig(pcfConfig).build();
    String criteria = pcfCommandValidation.getCriteria(request, null);
    assertThat(criteria).isEqualTo("Pcf:url/user");

    request = PcfCommandSetupRequest.builder().pcfConfig(pcfConfig).useCLIForPcfAppCreation(true).build();
    criteria = pcfCommandValidation.getCriteria(request, null);
    assertThat(criteria).isEqualTo("CF_CLI_INSTALLATION_REQUIRED_Pcf:url/user");

    request = PcfCommandSetupRequest.builder()
                  .pcfConfig(pcfConfig)
                  .useCLIForPcfAppCreation(true)
                  .useAppAutoscalar(true)
                  .build();
    criteria = pcfCommandValidation.getCriteria(request, null);
    assertThat(criteria).isEqualTo("CF_CLI_INSTALLATION_REQUIRED_cf_appautoscalar_Pcf:url/user");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testPcfCliValidationRequired() {
    Consumer consumer = mock(Consumer.class);
    PCFCommandValidation pcfCommandValidation =
        new PCFCommandValidation("", DelegateTaskPackage.builder().data(TaskData.builder().build()).build(), consumer);

    PcfConfig pcfConfig = PcfConfig.builder().endpointUrl("url").username("user".toCharArray()).build();
    PcfCommandSetupRequest request = PcfCommandSetupRequest.builder().pcfConfig(pcfConfig).build();
    assertThat(pcfCommandValidation.pcfCliValidationRequired(request)).isFalse();

    request.setUseAppAutoscalar(true);
    assertThat(pcfCommandValidation.pcfCliValidationRequired(request)).isTrue();

    request.setUseCfCLI(true);
    assertThat(pcfCommandValidation.pcfCliValidationRequired(request)).isTrue();

    PcfCommandDeployRequest deployRequest = PcfCommandDeployRequest.builder().pcfConfig(pcfConfig).build();
    assertThat(pcfCommandValidation.pcfCliValidationRequired(deployRequest)).isFalse();

    deployRequest.setUseAppAutoscalar(true);
    assertThat(pcfCommandValidation.pcfCliValidationRequired(deployRequest)).isTrue();

    deployRequest.setUseAppAutoscalar(false);
    deployRequest.setUseCfCLI(true);
    assertThat(pcfCommandValidation.pcfCliValidationRequired(deployRequest)).isTrue();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetCriteriaWithEncryptionDetails() {
    Consumer consumer = mock(Consumer.class);
    EncryptionService encryptionService = mock(EncryptionService.class);
    List<EncryptedDataDetail> encryptedDetails = Collections.emptyList();
    PCFCommandValidation pcfCommandValidation =
        new PCFCommandValidation("", DelegateTaskPackage.builder().data(TaskData.builder().build()).build(), consumer);
    on(pcfCommandValidation).set("encryptionService", encryptionService);

    PcfConfig pcfConfig = PcfConfig.builder().endpointUrl("url").username("user".toCharArray()).build();
    PcfCommandRequest request = PcfCommandSetupRequest.builder().pcfConfig(pcfConfig).build();
    String criteria = pcfCommandValidation.getCriteria(request, encryptedDetails);
    verify(encryptionService, times(1)).decrypt(pcfConfig, encryptedDetails);
    assertThat(criteria).isEqualTo("Pcf:url/user");
  }
}
