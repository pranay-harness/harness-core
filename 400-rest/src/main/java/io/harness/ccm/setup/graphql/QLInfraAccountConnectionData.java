package io.harness.ccm.setup.graphql;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.LINKED_ACCOUNT)
public class QLInfraAccountConnectionData implements QLObject {
  private String externalId;
  private String harnessAccountId;
  private String masterAccountCloudFormationTemplateLink;
  private String linkedAccountCloudFormationTemplateLink;
  private String masterAccountLaunchTemplateLink;
  private String linkedAccountLaunchTemplateLink;
}
