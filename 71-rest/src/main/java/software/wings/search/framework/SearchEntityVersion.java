package software.wings.search.framework;

import io.harness.persistence.PersistentEntity;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * The current version
 * of a search entity.
 *
 * @author utkarsh
 */

@Value
@Entity(value = "searchEntitiesVersion", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "SearchEntityVersionKeys")
@Slf4j
public class SearchEntityVersion implements PersistentEntity {
  @Id private String entityClass;
  private String syncVersion;

  private SearchEntity getSearchEntity() {
    try {
      return (SearchEntity) Class.forName(entityClass).newInstance();
    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
      logger.error("Could not create new instance", e);
    }
    return null;
  }

  boolean shouldBulkSync() {
    SearchEntity searchEntity = getSearchEntity();
    if (searchEntity != null) {
      return !searchEntity.getVersion().equals(syncVersion);
    }
    return true;
  }
}
