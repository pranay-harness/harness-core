package io.harness.accesscontrol.resources.resourcegroups;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

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

@OwnedBy(HarnessTeam.PL)
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class ResourceGroup {
  public static final String ALL_RESOURCES_IDENTIFIER = "*";

  final String scopeIdentifier;
  @NotEmpty final String identifier;
  @NotEmpty final String name;
  final Set<String> allowedScopeLevels;
  @NotNull final Set<String> resourceSelectors;
  final boolean fullScopeSelected;
  final boolean managed;
  @EqualsAndHashCode.Exclude @Setter Long createdAt;
  @EqualsAndHashCode.Exclude @Setter Long lastModifiedAt;
  @EqualsAndHashCode.Exclude @Setter Long version;
}
