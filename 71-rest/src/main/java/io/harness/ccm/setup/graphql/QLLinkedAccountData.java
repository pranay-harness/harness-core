package io.harness.ccm.setup.graphql;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.LINKED_ACCOUNT)
public class QLLinkedAccountData implements QLObject {
  private Integer count;
  private List<QLLinkedAccount> linkedAccounts;
}
