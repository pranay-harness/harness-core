package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@OwnedBy(HarnessTeam.PL)
@Data
@AllArgsConstructor
@Entity(value = "deletedEntities", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "DeletedEntityKeys")
@StoreIn(DbAliases.CG_MANAGER)
public class DeletedEntity implements PersistentEntity, PersistentRegularIterable {
  @Id private String uuid;
  @FdUniqueIndex private String entityId;
  private DeletedEntityType entityType;
  private int deletionAlgoNum;
  @FdIndex private long nextIteration;

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  public enum DeletedEntityType { ACCOUNT }
}
