/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.app.schema.type.delegate;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@OwnedBy(DEL)
@Value
@Builder
@AllArgsConstructor
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLDelegate implements QLObject {
  String uuid;
  String accountId;
  String delegateType;
  String delegateName;
  String hostName;
  String description;
  String ip;
  boolean pollingModeEnabled;
  String status;
  long lastHeartBeat;
  String version;
  String delegateProfileId;
}
