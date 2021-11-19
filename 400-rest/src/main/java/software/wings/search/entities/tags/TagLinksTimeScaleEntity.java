package software.wings.search.entities.tags;

import io.harness.persistence.PersistentEntity;

import software.wings.beans.HarnessTagLink;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.TimeScaleEntity;
import software.wings.timescale.migrations.MigrateTagLinksToTImeScaleDB;

import com.google.inject.Inject;
import java.util.Set;

public class TagLinksTimeScaleEntity implements TimeScaleEntity<HarnessTagLink> {
  @Inject private TagLinkTimescaleChangeHandler tagLinkTimescaleChangeHandler;
  @Inject private MigrateTagLinksToTImeScaleDB migrateTagLinksToTImeScaleDB;

  public static final Class<HarnessTagLink> SOURCE_ENTITY_CLASS = HarnessTagLink.class;

  @Override
  public Class<HarnessTagLink> getSourceEntityClass() {
    return SOURCE_ENTITY_CLASS;
  }

  @Override
  public ChangeHandler getChangeHandler() {
    return tagLinkTimescaleChangeHandler;
  }

  @Override
  public boolean toProcessChangeEvent(Set<String> accountIds, PersistentEntity entity) {
    HarnessTagLink harnessTagLink = (HarnessTagLink) entity;

    return accountIds.contains(harnessTagLink.getAccountId());
  }

  @Override
  public boolean runMigration(String accountId) {
    return migrateTagLinksToTImeScaleDB.runTimeScaleMigration(accountId);
  }
}
