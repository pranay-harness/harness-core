package io.harness.globalcontex;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PL)
public class EntityOperationIdentifier {
  public enum EntityOperation { CREATE, UPDATE, DELETE }

  private String entityType;
  private String entityName;
  private String entityId;
  private EntityOperation operation;

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
