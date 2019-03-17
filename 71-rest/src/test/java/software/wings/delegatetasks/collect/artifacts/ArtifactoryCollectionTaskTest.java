package software.wings.delegatetasks.collect.artifacts;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.waiter.ListNotifyResponseData;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.helpers.ext.artifactory.ArtifactoryService;

/**
 * Created by sgurubelli on 10/1/17.
 */
public class ArtifactoryCollectionTaskTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock ArtifactoryService artifactoryService;

  String url = "http://localhost:8881/artifactory/";

  private ArtifactoryConfig artifactoryConfig =
      ArtifactoryConfig.builder().artifactoryUrl(url).username("admin").password("dummy123!".toCharArray()).build();
  private DelegateTask collectionTask =
      DelegateTask.builder()
          .async(true)
          .taskType(TaskType.ARTIFACTORY_COLLECTION.name())
          .accountId(ACCOUNT_ID)
          .appId(APP_ID)
          .waitId("123456789")
          .data(TaskData.builder()
                    .parameters(new Object[] {artifactoryConfig.getArtifactoryUrl(), artifactoryConfig.getUsername(),
                        artifactoryConfig.getPassword(), "harness-maven", "io.harness.todolist", asList("todolist"), "",
                        ImmutableMap.of("buildNo", "1.1")})
                    .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                    .build())
          .build();

  @InjectMocks
  private ArtifactoryCollectionTask artifactoryCollectionTask =
      (ArtifactoryCollectionTask) TaskType.ARTIFACTORY_COLLECTION.getDelegateRunnableTask(
          "delid1", collectionTask, notifyResponseData -> {}, () -> true);

  @Test
  @Category(UnitTests.class)
  public void shouldCollectNoMavenStyleFiles() {
    ListNotifyResponseData res = ListNotifyResponseData.Builder.aListNotifyResponseData().build();
    when(artifactoryService.downloadArtifacts(
             any(ArtifactoryConfig.class), any(), anyString(), anyMap(), anyString(), anyString(), anyString()))
        .thenReturn(res);
    res = artifactoryCollectionTask.run(collectionTask.getData().getParameters());
    assertThat(res).isNotNull();
  }
}
