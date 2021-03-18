package io.harness.changestreamsframework;

import io.harness.persistence.PersistentEntity;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class ChangeTrackingInfo<T extends PersistentEntity> {
  private Class<T> morphiaClass;
  private ChangeSubscriber<T> changeSubscriber;
  private String resumeToken;
}
