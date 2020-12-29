package io.harness.pms.serializer.recaster;

import io.harness.core.Recast;

import lombok.experimental.UtilityClass;
import org.bson.Document;

@UtilityClass
public class RecastOrchestrationUtils {
  private static final Recast recast = new Recast();

  public <T> Document toDocument(T entity) {
    return recast.toDocument(entity);
  }

  public <T> T fromDocument(Document document, Class<T> entityClass) {
    return recast.fromDocument(document, entityClass);
  }
}
