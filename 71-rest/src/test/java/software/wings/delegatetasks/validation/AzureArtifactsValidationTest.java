package software.wings.delegatetasks.validation;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.GARVIT;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.AzureArtifactsArtifactStream.ProtocolType;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig;
import software.wings.delegatetasks.collect.artifacts.AzureArtifactsCollectionTaskParameters;

import java.util.List;

public class AzureArtifactsValidationTest extends WingsBaseTest {
  private static final String AZURE_DEVOPS_URL1 = "https://dev.azure.com/ORG1";
  private static final String AZURE_DEVOPS_URL2 = "https://dev.azure.com/ORG2";

  private static DelegateTask collectionDelegateTask =
      DelegateTask.builder()
          .accountId(ACCOUNT_ID)
          .appId(APP_ID)
          .waitId("waitId")
          .data(TaskData.builder()
                    .async(true)
                    .taskType(TaskType.AZURE_ARTIFACTS_COLLECTION.name())
                    .parameters(new Object[] {
                        AzureArtifactsCollectionTaskParameters.builder()
                            .accountId(ACCOUNT_ID)
                            .azureArtifactsConfig(
                                AzureArtifactsPATConfig.builder().azureDevopsUrl(AZURE_DEVOPS_URL1).build())
                            .encryptedDataDetails(null)
                            .artifactStreamAttributes(ArtifactStreamAttributes.builder()
                                                          .protocolType(ProtocolType.maven.name())
                                                          .project("PROJECT")
                                                          .feed("FEED")
                                                          .packageId("PACKAGE_ID")
                                                          .packageName("GROUP_ID:ARTIFACT_ID")
                                                          .build())
                            .artifactMetadata(ImmutableMap.of("buildNo", "1.0", "version", "1.0", "versionId", "VID"))
                            .build()})
                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                    .build())
          .build();

  private static DelegateTask getBuildsDelegateTask =
      DelegateTask.builder()
          .uuid("uuid")
          .accountId(ACCOUNT_ID)
          .appId(APP_ID)
          .waitId("waitId")
          .data(TaskData.builder()
                    .async(true)
                    .taskType(TaskType.AZURE_ARTIFACTS_GET_BUILDS.name())
                    .parameters(new Object[] {ArtifactStreamAttributes.builder()
                                                  .protocolType(ProtocolType.maven.name())
                                                  .project("PROJECT")
                                                  .feed("FEED")
                                                  .packageId("PACKAGE_ID")
                                                  .packageName("GROUP_ID:ARTIFACT_ID")
                                                  .build(),
                        singletonList(EncryptedDataDetail.builder().build()),
                        AzureArtifactsPATConfig.builder().azureDevopsUrl(AZURE_DEVOPS_URL2).build()})
                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                    .build())
          .build();

  @InjectMocks
  private AzureArtifactsValidation azureArtifactsCollectionValidation =
      new AzureArtifactsValidation(DELEGATE_ID, collectionDelegateTask, null);

  @InjectMocks
  private AzureArtifactsValidation azureArtifactsGetBuildsValidation =
      new AzureArtifactsValidation(DELEGATE_ID, getBuildsDelegateTask, null);

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetCriteriaForCollectionTask() {
    List<String> criteria = azureArtifactsCollectionValidation.getCriteria();
    assertThat(criteria).hasSize(1);
    assertThat(criteria.get(0)).isEqualTo(AZURE_DEVOPS_URL1);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetCriteriaForGetBuildsTask() {
    List<String> criteria = azureArtifactsGetBuildsValidation.getCriteria();
    assertThat(criteria).hasSize(1);
    assertThat(criteria.get(0)).isEqualTo(AZURE_DEVOPS_URL2);
  }
}