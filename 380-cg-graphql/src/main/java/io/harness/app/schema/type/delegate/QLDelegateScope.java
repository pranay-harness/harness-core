package io.harness.app.schema.type.delegate;

import software.wings.graphql.schema.type.QLEnvironmentType;
import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLDelegateScope implements QLObject {
  String name;
  String accountId;
  String uuid;
  List<QLTaskGroup> taskTypes;
  List<QLEnvironmentType> environmentTypes;
  List<String> applications;
  List<String> environments;
  List<String> services;
  List<String> infrastructureDefinitions;
}
