package io.harness.globalcontex;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EntityOperationIdentifier {
  public enum entityOperation { CREATE, UPDATE, DELETE }
  ;
  private String entityType;
  private String entityName;
  private String entityId;
  private entityOperation operation;

  @Override
  public String toString() {
    return new StringBuilder(128).append(entityType).append(entityName).append(entityId).append(operation).toString();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof EntityOperationIdentifier)) {
      return false;
    }

    return toString().equals(o.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }
}
