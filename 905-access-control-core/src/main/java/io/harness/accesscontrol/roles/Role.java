package io.harness.accesscontrol.roles;

import io.harness.accesscontrol.roles.validator.ValidRole;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;

import java.util.Map;
import java.util.Set;
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
@ValidRole
public class Role {
  @EntityIdentifier final String identifier;
  final String scopeIdentifier;
  @NGEntityName final String name;
  @NotEmpty final Set<String> allowedScopeLevels;
  @NotNull final Set<String> permissions;
  final boolean managed;
  final String description;
  final Map<String, String> tags;
  @EqualsAndHashCode.Exclude @Setter Long createdAt;
  @EqualsAndHashCode.Exclude @Setter Long lastModifiedAt;
  @EqualsAndHashCode.Exclude @Setter Long version;
}
