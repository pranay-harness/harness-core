package io.harness.ccm.setup.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.LINKED_ACCOUNT)
@OwnedBy(CE)
public class QLLinkedAccountData implements QLObject {
  private QLAccountCountStats count;
  private List<QLLinkedAccount> linkedAccounts;
}
