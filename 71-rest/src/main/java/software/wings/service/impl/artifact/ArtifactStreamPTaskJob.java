package software.wings.service.impl.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.beans.FeatureName.ARTIFACT_PERPETUAL_TASK;
import static software.wings.beans.FeatureName.ARTIFACT_PERPETUAL_TASK_MIGRATION;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class ArtifactStreamPTaskJob implements Managed {
  private static final int BATCH_SIZE = 100;

  @Inject private ArtifactStreamPTaskHelper artifactStreamPTaskHelper;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FeatureFlagService featureFlagService;

  private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactoryBuilder().setNameFormat("artifact-stream-perpetual-task-job").build());
  private Future<?> artifactStreamPTaskJobFuture;

  @Override
  public void start() {
    artifactStreamPTaskJobFuture = executorService.scheduleWithFixedDelay(this ::run, 0, 10, TimeUnit.MINUTES);
  }

  @Override
  public void stop() throws InterruptedException {
    if (artifactStreamPTaskJobFuture != null) {
      artifactStreamPTaskJobFuture.cancel(true);
    }

    executorService.shutdown();
    executorService.awaitTermination(2, TimeUnit.SECONDS);
  }

  @VisibleForTesting
  public void run() {
    if (isGloballyEnabled()) {
      createPerpetualTasks(null);
      return;
    }

    Set<String> accountIds = getAccountIds();
    if (isEmpty(accountIds)) {
      return;
    }

    createPerpetualTasks(accountIds);
  }

  private void createPerpetualTasks(Set<String> accountIds) {
    Query<ArtifactStream> query;
    if (isEmpty(accountIds)) {
      query = wingsPersistence.createQuery(ArtifactStream.class, excludeAuthority);
    } else {
      query = wingsPersistence.createQuery(ArtifactStream.class).field(ArtifactStreamKeys.accountId).in(accountIds);
    }

    List<ArtifactStream> artifactStreams = query.field(ArtifactStreamKeys.perpetualTaskId)
                                               .doesNotExist()
                                               .project(ArtifactStreamKeys.accountId, true)
                                               .project(ArtifactStreamKeys.uuid, true)
                                               .asList(new FindOptions().limit(BATCH_SIZE));
    if (isEmpty(artifactStreams)) {
      return;
    }

    artifactStreams.forEach(artifactStream -> artifactStreamPTaskHelper.createPerpetualTask(artifactStream));
  }

  private boolean isGloballyEnabled() {
    return featureFlagService.isGlobalEnabled(ARTIFACT_PERPETUAL_TASK_MIGRATION)
        && featureFlagService.isGlobalEnabled(ARTIFACT_PERPETUAL_TASK);
  }

  private Set<String> getAccountIds() {
    Set<String> accountIdsForMigration = featureFlagService.getAccountIds(ARTIFACT_PERPETUAL_TASK_MIGRATION);
    if (isEmpty(accountIdsForMigration)) {
      return Collections.emptySet();
    }

    Set<String> accountIds = featureFlagService.getAccountIds(ARTIFACT_PERPETUAL_TASK);
    if (isEmpty(accountIds)) {
      return Collections.emptySet();
    }

    return Sets.intersection(accountIdsForMigration, accountIds);
  }
}
