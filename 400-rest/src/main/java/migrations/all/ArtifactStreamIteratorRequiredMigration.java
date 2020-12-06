package migrations.all;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;

@Slf4j
public class ArtifactStreamIteratorRequiredMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    wingsPersistence.update(
        wingsPersistence.createQuery(ArtifactStream.class).filter(ArtifactStreamKeys.nextIteration, null),
        wingsPersistence.createUpdateOperations(ArtifactStream.class).set(ArtifactStreamKeys.nextIteration, 0));
    wingsPersistence.update(
        wingsPersistence.createQuery(ArtifactStream.class).filter(ArtifactStreamKeys.nextCleanupIteration, null),
        wingsPersistence.createUpdateOperations(ArtifactStream.class).set(ArtifactStreamKeys.nextCleanupIteration, 0));
  }
}
