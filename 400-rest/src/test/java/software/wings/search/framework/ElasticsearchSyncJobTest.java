/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.search.framework;

import static io.harness.rule.OwnerRule.UTKARSH;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayDeque;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ElasticsearchSyncJobTest extends WingsBaseTest {
  @Mock Provider<ElasticsearchBulkSyncTask> elasticsearchBulkSyncTaskProvider;
  @Mock Provider<ElasticsearchRealtimeSyncTask> elasticsearchRealtimeSyncTaskProvider;
  @Inject @InjectMocks ElasticsearchSyncJob elasticsearchSyncJob;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testElasticsearchSyncTask() throws InterruptedException {
    ElasticsearchBulkSyncTask elasticsearchBulkSyncTask = mock(ElasticsearchBulkSyncTask.class);
    ElasticsearchRealtimeSyncTask elasticsearchRealtimeSyncTask = mock(ElasticsearchRealtimeSyncTask.class);

    when(elasticsearchBulkSyncTaskProvider.get()).thenReturn(elasticsearchBulkSyncTask);
    when(elasticsearchRealtimeSyncTaskProvider.get()).thenReturn(elasticsearchRealtimeSyncTask);

    ElasticsearchBulkSyncTaskResult elasticsearchBulkSyncTaskResult =
        ElasticsearchBulkSyncTaskResult.builder()
            .isSuccessful(true)
            .changeEventsDuringBulkSync(new ArrayDeque<>())
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
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  @Ignore("Investigate to make sure Search Unit Tests are not creating system resources such as Threads")
  public void testErroredElasticsearchSyncTask() throws InterruptedException {
    ElasticsearchBulkSyncTask elasticsearchBulkSyncTask = mock(ElasticsearchBulkSyncTask.class);
    ElasticsearchRealtimeSyncTask elasticsearchRealtimeSyncTask = mock(ElasticsearchRealtimeSyncTask.class);

    when(elasticsearchBulkSyncTaskProvider.get()).thenReturn(elasticsearchBulkSyncTask);
    when(elasticsearchRealtimeSyncTaskProvider.get()).thenReturn(elasticsearchRealtimeSyncTask);

    ElasticsearchBulkSyncTaskResult elasticsearchBulkSyncTaskResult =
        ElasticsearchBulkSyncTaskResult.builder()
            .isSuccessful(true)
            .changeEventsDuringBulkSync(new ArrayDeque<>())
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
