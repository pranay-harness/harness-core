/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;
import io.harness.ng.core.notification.NotificationSettingConfigDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@Schema(name = "UserGroup", description = "This is the view of the UserGroup entity defined in Harness")
public class UserGroupDTO {
  String accountIdentifier;
  @EntityIdentifier(allowBlank = true) String orgIdentifier;
  @EntityIdentifier(allowBlank = true) String projectIdentifier;
  @NotNull @EntityIdentifier String identifier;

  @NotNull @NGEntityName String name;
  List<String> users;
  List<NotificationSettingConfigDTO> notificationConfigs;

  private boolean isSsoLinked;
  private String linkedSsoId;
  private String linkedSsoDisplayName;
  private String ssoGroupId;
  private String ssoGroupName;

  private boolean externallyManaged;

  @Size(max = 1024) String description;
  @Size(max = 128) Map<String, String> tags;
}
