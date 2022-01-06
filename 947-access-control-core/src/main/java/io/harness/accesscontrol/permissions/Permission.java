/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.permissions;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.permissions.validator.PermissionIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.NGEntityName;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "Permission")
public class Permission {
  public static final String PERMISSION_DELIMITER = "_";

  @PermissionIdentifier String identifier;
  @NGEntityName String name;
  @NotNull PermissionStatus status;
  boolean includeInAllRoles;
  @NotEmpty Set<String> allowedScopeLevels;
  @EqualsAndHashCode.Exclude @Setter Long version;

  public String getPermissionMetadata(int index) {
    List<String> permissionMetadata = Arrays.asList(identifier.split(PERMISSION_DELIMITER));
    return permissionMetadata.get(index);
  }
}
