package software.wings.delegatetasks.collect.artifacts;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.common.collect.ImmutableMap;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.helpers.ext.artifactory.ArtifactoryService;
import software.wings.waitnotify.ListNotifyResponseData;

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
      DelegateTask.Builder.aDelegateTask()
          .withTaskType(TaskType.ARTIFACTORY_COLLECTION)
          .withAccountId(ACCOUNT_ID)
          .withAppId(APP_ID)
          .withWaitId("123456789")
          .withParameters(new Object[] {artifactoryConfig.getArtifactoryUrl(), artifactoryConfig.getUsername(),
              artifactoryConfig.getPassword(), "harness-maven", "io.harness.todolist", asList("todolist"), "",
              ImmutableMap.of("buildNo", "1.1")})
          .build();

  @InjectMocks
  private ArtifactoryCollectionTask artifactoryCollectionTask =
      (ArtifactoryCollectionTask) TaskType.ARTIFACTORY_COLLECTION.getDelegateRunnableTask(
          "delid1", collectionTask, notifyResponseData -> {}, () -> true);

  @Test
  public void shouldCollectNoMavenStyleFiles() {
    ListNotifyResponseData res = ListNotifyResponseData.Builder.aListNotifyResponseData().build();
    when(artifactoryService.downloadArtifacts(any(ArtifactoryConfig.class), any(), anyString(), anyString(), anyList(),
             anyString(), anyMap(), anyString(), anyString(), anyString()))
        .thenReturn(res);
    res = artifactoryCollectionTask.run(collectionTask.getParameters());
    assertThat(res).isNotNull();
  }
}
