/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

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

  @NotEmpty final String scopeIdentifier;
  @NotEmpty final String identifier;
  @NotEmpty final String name;
  @NotNull final Set<String> resourceSelectors;
  final boolean fullScopeSelected;
  final boolean managed;
  @EqualsAndHashCode.Exclude @Setter Long createdAt;
  @EqualsAndHashCode.Exclude @Setter Long lastModifiedAt;
  @EqualsAndHashCode.Exclude @Setter Long version;
}
