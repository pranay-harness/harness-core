/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.userGroup;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.sso.SSOType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class UserGroupSSOSettings {
  boolean isSSOLinked;
  String linkedSSOId;
  SSOType ssoType;
  String ssoGroupName;
  String linkedSsoDisplayName;
  String ssoGroupId;
}
