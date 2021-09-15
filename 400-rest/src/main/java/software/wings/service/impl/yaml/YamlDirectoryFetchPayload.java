/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml;

import software.wings.security.UserPermissionInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class YamlDirectoryFetchPayload {
  private String accountId;
  private String appId;
  private String entityId;
  private boolean applyPermissions;
  private UserPermissionInfo userPermissionInfo;
  private boolean appLevelYamlTreeOnly;
  private boolean addApplication;
}
