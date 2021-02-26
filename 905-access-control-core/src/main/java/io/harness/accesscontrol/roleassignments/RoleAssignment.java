package io.harness.accesscontrol.roleassignments;

import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.data.validator.EntityIdentifier;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class RoleAssignment {
  @EntityIdentifier final String identifier;
  @NotEmpty final String scopeIdentifier;
  @NotEmpty final String resourceGroupIdentifier;
  @NotEmpty final String roleIdentifier;
  @NotEmpty final String principalIdentifier;
  @NotNull final PrincipalType principalType;
  final String description;
  final Map<String, String> tags;
  @Setter boolean managed;
  @Setter boolean disabled;
  final Long createdAt;
  final Long lastModifiedAt;
}
