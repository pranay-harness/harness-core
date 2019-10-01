package io.harness.waiter;

import io.harness.annotation.HarnessEntity;
import io.harness.delegate.beans.ResponseData;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.Map;

/**
 * Represents errors thrown by callback of wait instance.
 */
@Value
@Builder
@Entity(value = "waitInstanceErrors", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class WaitInstanceError implements PersistentEntity {
  @Id private String waitInstanceId;
  private Map<String, ResponseData> responseMap;
  private String errorStackTrace;
}
