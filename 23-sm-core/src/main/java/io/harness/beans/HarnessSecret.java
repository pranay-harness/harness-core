package io.harness.beans;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.ScopedEntity;
import software.wings.security.UsageRestrictions;

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode
@FieldNameConstants(innerTypeName = "HarnessSecretKeys")
public class HarnessSecret implements ScopedEntity {
  @NotEmpty String name;
  String kmsId;
  UsageRestrictions usageRestrictions;
  boolean scopedToAccount;
  boolean hideFromListing;
  boolean inheritScopesFromSM;
  Map<String, String> runtimeParameters;
}
