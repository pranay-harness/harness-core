package io.harness.migration.entities;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityName;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

@OwnedBy(DX)
public class NGSchema {
  @JsonIgnore @Id @org.mongodb.morphia.annotations.Id String id;
  @NotEmpty @CreatedDate Long createdAt;
  @LastModifiedDate Long lastUpdatedAt;
  @NotEmpty @EntityName String name;
  Map<String, Long> migrationDetails;
}
