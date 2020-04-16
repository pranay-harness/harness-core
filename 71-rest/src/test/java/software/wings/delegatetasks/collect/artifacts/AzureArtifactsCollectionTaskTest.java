package software.wings.delegatetasks.collect.artifacts;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.GARVIT;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.TaskParameters;
import io.harness.rule.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.AzureArtifactsArtifactStream.ProtocolType;
import software.wings.beans.settings.azureartifacts.AzureArtifactsConfig;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig;
import software.wings.helpers.ext.azure.devops.AzureArtifactsService;

public class AzureArtifactsCollectionTaskTest extends CategoryTest {
  private static final String DELEGATE_ID1 = "DELEGATE_ID1";
  private static final String DELEGATE_ID2 = "DELEGATE_ID2";
  private static final String VERSION_ID = "VERSION_ID";
  private static final String VERSION = "VERSION";

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock AzureArtifactsService azureArtifactsService;

  private String azureDevopsUrl = "http://localhost:8881/artifactory/";
  private AzureArtifactsPATConfig azureArtifactsPATConfig =
      AzureArtifactsPATConfig.builder().azureDevopsUrl(azureDevopsUrl).pat("dummy123!".toCharArray()).build();
  private DelegateTask mavenDelegateTask = prepareDelegateTask(ProtocolType.maven);
  private DelegateTask nugetDelegateTask = prepareDelegateTask(ProtocolType.nuget);

  @InjectMocks
  private AzureArtifactsCollectionTask mavenCollectionTask =
      (AzureArtifactsCollectionTask) TaskType.AZURE_ARTIFACTS_COLLECTION.getDelegateRunnableTask(
          DELEGATE_ID1, mavenDelegateTask, notifyResponseData -> {}, () -> true);

  @InjectMocks
  private AzureArtifactsCollectionTask nugetCollectionTask =
      (AzureArtifactsCollectionTask) TaskType.AZURE_ARTIFACTS_COLLECTION.getDelegateRunnableTask(
          DELEGATE_ID2, nugetDelegateTask, notifyResponseData -> {}, () -> true);

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldCollectMavenArtifact() {
    mavenCollectionTask.run(mavenDelegateTask.getData().getParameters());
    verify(azureArtifactsService, times(1))
        .downloadArtifact(
            any(AzureArtifactsConfig.class), any(), any(), any(), eq(DELEGATE_ID1), any(), eq(ACCOUNT_ID), any());
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldCollectMavenArtifactAsTaskParameter() {
    mavenCollectionTask.run((TaskParameters) mavenDelegateTask.getData().getParameters()[0]);
    verify(azureArtifactsService, times(1))
        .downloadArtifact(
            any(AzureArtifactsConfig.class), any(), any(), any(), eq(DELEGATE_ID1), any(), eq(ACCOUNT_ID), any());
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldCollectNugetArtifact() {
    nugetCollectionTask.run(nugetDelegateTask.getData().getParameters());
    verify(azureArtifactsService, times(1))
        .downloadArtifact(
            any(AzureArtifactsConfig.class), any(), any(), any(), eq(DELEGATE_ID2), any(), eq(ACCOUNT_ID), any());
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldCollectNugetArtifactAsTaskParameter() {
    nugetCollectionTask.run((TaskParameters) nugetDelegateTask.getData().getParameters()[0]);
    verify(azureArtifactsService, times(1))
        .downloadArtifact(
            any(AzureArtifactsConfig.class), any(), any(), any(), eq(DELEGATE_ID2), any(), eq(ACCOUNT_ID), any());
  }

  private DelegateTask prepareDelegateTask(ProtocolType protocolType) {
    return DelegateTask.builder()
        .accountId(ACCOUNT_ID)
        .appId(APP_ID)
        .waitId("123456789")
        .data(TaskData.builder()
                  .async(true)
                  .taskType(TaskType.AZURE_ARTIFACTS_COLLECTION.name())
                  .parameters(new Object[] {
                      AzureArtifactsCollectionTaskParameters.builder()
                          .accountId(ACCOUNT_ID)
                          .azureArtifactsConfig(azureArtifactsPATConfig)
                          .encryptedDataDetails(null)
                          .artifactStreamAttributes(
                              ArtifactStreamAttributes.builder()
                                  .protocolType(protocolType.name())
                                  .project("PROJECT")
                                  .feed("FEED")
                                  .packageId("PACKAGE_ID")
                                  .packageName(
                                      protocolType == ProtocolType.nuget ? "PACKAGE_NAME" : "GROUP_ID:ARTIFACT_ID")
                                  .build())
                          .artifactMetadata(
                              ImmutableMap.of("buildNo", VERSION, "version", VERSION, "versionId", VERSION_ID))
                          .build()})
                  .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                  .build())
        .build();
  }
}
