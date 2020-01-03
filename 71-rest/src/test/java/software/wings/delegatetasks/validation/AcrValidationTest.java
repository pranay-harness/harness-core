package software.wings.delegatetasks.validation;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.PUNEET;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.AzureConfig;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;

import java.util.List;

public class AcrValidationTest extends WingsBaseTest {
  @InjectMocks private AcrValidation acrValidation = new AcrValidation(DELEGATE_ID, delegateTask, null);

  private static final String repositoryName = "my-repository";
  private static final String registryName = "my-registry";

  private static final String ACR_URL = "https://azure.microsoft.com/";

  static DelegateTask delegateTask =
      DelegateTask.builder()
          .uuid("id")
          .async(true)
          .accountId(ACCOUNT_ID)
          .appId(APP_ID)
          .waitId("")
          .data(TaskData.builder()
                    .taskType(TaskType.ACR_GET_BUILDS.name())
                    .parameters(new Object[] {ArtifactStreamAttributes.builder()
                                                  .registryName(registryName)
                                                  .repositoryName(repositoryName)
                                                  .build(),
                        asList(EncryptedDataDetail.builder().build()), AzureConfig.builder().build()})
                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                    .build())
          .build();

  @Before
  public void setUp() throws Exception {}

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void getCriteriaTest() {
    List<String> criteria = acrValidation.getCriteria();
    assertThat(criteria).hasSize(1);
    assertThat(criteria.get(0)).isEqualTo(ACR_URL);
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void validateSuccessTest() {
    List<DelegateConnectionResult> result = acrValidation.validate();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).isValidated()).isTrue();
  }
}