/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.graphql.schema.mutation.userGroup.input;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.utils.RequestField;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.type.permissions.QLUserGroupPermissions;
import software.wings.graphql.schema.type.usergroup.QLNotificationSettings;
import software.wings.graphql.schema.type.usergroup.QLSSOSettingInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUpdateUserGroupInputKeys")
@Scope(PermissionAttribute.ResourceType.USER)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLUpdateUserGroupInput implements QLMutationInput {
  String clientMutationId;
  RequestField<String> name;
  RequestField<String> description;
  String userGroupId;
  RequestField<List<String>> userIds;
  RequestField<QLSSOSettingInput> ssoSetting;
  RequestField<QLUserGroupPermissions> permissions;
  RequestField<QLNotificationSettings> notificationSettings;
}
