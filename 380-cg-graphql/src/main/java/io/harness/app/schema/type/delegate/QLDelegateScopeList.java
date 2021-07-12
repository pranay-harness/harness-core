package io.harness.app.schema.type.delegate;

import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLDelegateScopeList {
  private QLPageInfo pageInfo;
  @Singular private List<QLDelegateScope> nodes;
}
