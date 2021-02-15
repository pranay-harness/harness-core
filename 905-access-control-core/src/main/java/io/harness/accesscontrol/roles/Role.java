package io.harness.accesscontrol.roles;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;

import java.util.Map;
import java.util.Set;
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
public class Role {
  @EntityIdentifier final String identifier;
  final String scopeIdentifier;
  @NGEntityName final String name;
  @NotEmpty final Set<String> allowedScopeLevels;
  @NotEmpty final Set<String> permissions;
  @Setter boolean managed;
  final String description;
  final Map<String, String> tags;
  final Long createdAt;
  final Long lastModifiedAt;
}
