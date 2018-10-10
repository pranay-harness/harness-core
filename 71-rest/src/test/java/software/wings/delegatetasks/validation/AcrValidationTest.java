package software.wings.delegatetasks.validation;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.when;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AzureConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.azure.AcrService;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

public class AcrValidationTest extends WingsBaseTest {
  @Mock AcrService acrService;

  @InjectMocks private AcrValidation acrValidation = new AcrValidation(DELEGATE_ID, delegateTask, null);

  private static final String repositoryName = "my-repository";
  private static final String registryName = "my-registry";

  static DelegateTask delegateTask =
      aDelegateTask()
          .withUuid("id")
          .withAccountId(ACCOUNT_ID)
          .withAppId(APP_ID)
          .withTaskType(TaskType.ACR_GET_BUILDS)
          .withWaitId("")
          .withParameters(new Object[] {ArtifactStreamAttributes.Builder.anArtifactStreamAttributes()
                                            .withRegistryName(registryName)
                                            .withRepositoryName(repositoryName)
                                            .build(),
              asList(EncryptedDataDetail.builder().build()), AzureConfig.builder().build()})
          .build();

  @Before
  public void setUp() throws Exception {}

  @Test
  public void getCriteriaTest() {
    List<String> criteria = acrValidation.getCriteria();
    assertThat(criteria).hasSize(1);
    assertThat(criteria.get(0)).isEqualTo("ACR_my-registry_my-repository");
  }

  @Test
  public void validateSuccessTest() {
    when(acrService.validateCredentials(any(), anyList(), any())).thenReturn(true);
    List<DelegateConnectionResult> result = acrValidation.validate();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).isValidated()).isTrue();
  }

  @Test
  public void validateFailureTest() {
    when(acrService.validateCredentials(any(), anyList(), any())).thenReturn(false);
    List<DelegateConnectionResult> result = acrValidation.validate();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).isValidated()).isFalse();
  }
}