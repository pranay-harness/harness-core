/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.roles;

import io.harness.accesscontrol.roles.validator.ValidRole;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
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

@OwnedBy(HarnessTeam.PL)
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
