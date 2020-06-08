package io.harness.ng.core.entities;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Value
@Builder
@FieldNameConstants(innerTypeName = "OrganizationKeys")
public class Organization {
  @Wither @Id @org.mongodb.morphia.annotations.Id String id;
  @Trimmed @NotEmpty String accountId;

  @NotEmpty @EntityIdentifier String identifier;
  @NotEmpty @EntityName String name;
  @NotEmpty String color;
  @NotNull @Size(max = 1024) String description;
  @NotNull @Singular @Size(max = 128) List<String> tags;
  // TODO{phoenikx} Add collaborators once there is clarity on the same

  @Wither @CreatedDate Long createdAt;
  @Wither @LastModifiedDate Long lastModifiedAt;
  @Wither @Version Long version;
}
