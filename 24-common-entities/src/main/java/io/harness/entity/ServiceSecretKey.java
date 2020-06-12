package io.harness.entity;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "ServiceSecretKeyKeys")
@Entity(value = "serviceSecrets", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class ServiceSecretKey implements PersistentEntity, CreatedAtAware, UpdatedAtAware {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  private String serviceSecret;

  @Indexed(options = @IndexOptions(unique = true)) private ServiceType serviceType;

  public enum ServiceType { LEARNING_ENGINE }

  // add version in the end
  public enum ServiceApiVersion { V1 }
}
