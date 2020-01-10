package software.wings.graphql.schema.type.permissions;

import software.wings.graphql.schema.type.QLEnum;

public enum QLAccountPermissionType implements QLEnum {
  CREATE_AND_DELETE_APPLICATION,
  READ_USERS_AND_GROUPS,
  MANAGE_USERS_AND_GROUPS,
  MANAGE_TEMPLATE_LIBRARY,
  ADMINISTER_OTHER_ACCOUNT_FUNCTIONS,
  VIEW_AUDITS,
  MANAGE_TAGS;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
