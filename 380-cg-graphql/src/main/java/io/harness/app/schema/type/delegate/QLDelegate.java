package io.harness.app.schema.type.delegate;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLDelegate implements QLObject {
  String id;
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
