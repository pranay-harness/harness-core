package software.wings.search.framework;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;
import com.google.inject.Provider;

import io.harness.category.element.UnitTests;
import org.eclipse.jetty.util.ArrayQueue;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

public class ElasticsearchSyncJobTest extends WingsBaseTest {
  @Mock Provider<ElasticsearchBulkSyncTask> elasticsearchBulkSyncTaskProvider;
  @Mock Provider<ElasticsearchRealtimeSyncTask> elasticsearchRealtimeSyncTaskProvider;
  @Inject @InjectMocks ElasticsearchSyncJob elasticsearchSyncJob;

  @Test
  @Category(UnitTests.class)
  public void testElasticsearchSyncTask() {
    ElasticsearchBulkSyncTask elasticsearchBulkSyncTask = mock(ElasticsearchBulkSyncTask.class);
    ElasticsearchRealtimeSyncTask elasticsearchRealtimeSyncTask = mock(ElasticsearchRealtimeSyncTask.class);

    when(elasticsearchBulkSyncTaskProvider.get()).thenReturn(elasticsearchBulkSyncTask);
    when(elasticsearchRealtimeSyncTaskProvider.get()).thenReturn(elasticsearchRealtimeSyncTask);

    ElasticsearchBulkSyncTaskResult elasticsearchBulkSyncTaskResult =
        ElasticsearchBulkSyncTaskResult.builder()
            .isSuccessful(true)
            .changeEventsDuringBulkSync(new ArrayQueue<>())
            .build();

    when(elasticsearchBulkSyncTask.run()).thenReturn(elasticsearchBulkSyncTaskResult);
    when(elasticsearchRealtimeSyncTask.run(elasticsearchBulkSyncTaskResult.getChangeEventsDuringBulkSync()))
        .thenReturn(false);
    doNothing().when(elasticsearchRealtimeSyncTask).stop();

    elasticsearchSyncJob.run();
    verify(elasticsearchBulkSyncTask, times(1)).run();
    verify(elasticsearchRealtimeSyncTask, times(1))
        .run(elasticsearchBulkSyncTaskResult.getChangeEventsDuringBulkSync());
    verify(elasticsearchRealtimeSyncTask, times(1)).stop();
  }

  @Test
  @Category(UnitTests.class)
  public void testErroredElasticsearchSyncTask() {
    ElasticsearchBulkSyncTask elasticsearchBulkSyncTask = mock(ElasticsearchBulkSyncTask.class);
    ElasticsearchRealtimeSyncTask elasticsearchRealtimeSyncTask = mock(ElasticsearchRealtimeSyncTask.class);

    when(elasticsearchBulkSyncTaskProvider.get()).thenReturn(elasticsearchBulkSyncTask);
    when(elasticsearchRealtimeSyncTaskProvider.get()).thenReturn(elasticsearchRealtimeSyncTask);

    ElasticsearchBulkSyncTaskResult elasticsearchBulkSyncTaskResult =
        ElasticsearchBulkSyncTaskResult.builder()
            .isSuccessful(true)
            .changeEventsDuringBulkSync(new ArrayQueue<>())
            .build();

    when(elasticsearchBulkSyncTask.run()).thenReturn(elasticsearchBulkSyncTaskResult);
    when(elasticsearchRealtimeSyncTask.run(elasticsearchBulkSyncTaskResult.getChangeEventsDuringBulkSync()))
        .thenThrow(new RuntimeException());
    doNothing().when(elasticsearchRealtimeSyncTask).stop();

    elasticsearchSyncJob.run();
    verify(elasticsearchBulkSyncTask, times(1)).run();
    verify(elasticsearchRealtimeSyncTask, times(1))
        .run(elasticsearchBulkSyncTaskResult.getChangeEventsDuringBulkSync());
    verify(elasticsearchRealtimeSyncTask, times(1)).stop();
  }
}
