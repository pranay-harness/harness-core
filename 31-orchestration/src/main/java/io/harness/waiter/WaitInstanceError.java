package io.harness.waiter;

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
@Entity(value = "waitInstanceErrors", noClassnameStored = true)
@Value
@Builder
public class WaitInstanceError implements PersistentEntity {
  @Id private String waitInstanceId;
  private Map<String, ResponseData> responseMap;
  private String errorStackTrace;
}
