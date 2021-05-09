package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@OwnedBy(PL)
@Value
@Builder
@Entity(value = "elasticsearchPendingBulkMigrations", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "ElasticsearchBulkMigrationJobKeys")
@StoreIn(DbAliases.CG_MANAGER)
@Slf4j
public class ElasticsearchBulkMigrationJob implements PersistentEntity {
  @Id private String entityClass;
  private String newIndexName;
  private String oldIndexName;
  private String fromVersion;
  private String toVersion;
}
