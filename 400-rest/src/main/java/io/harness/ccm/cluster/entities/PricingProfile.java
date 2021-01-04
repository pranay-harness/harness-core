package io.harness.ccm.cluster.entities;

import io.harness.annotation.StoreIn;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@Entity(value = "pricingProfile")
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn("harness")
@FieldNameConstants(innerTypeName = "PricingProfileKeys")
public class PricingProfile implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id String uuid;
  String accountId;
  Double vCpuPricePerHr;
  Double memoryGbPricePerHr;
  long createdAt;
  long lastUpdatedAt;
}
