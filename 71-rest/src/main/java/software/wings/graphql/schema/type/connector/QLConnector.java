package software.wings.graphql.schema.type.connector;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Scope(ResourceType.SETTING)
public interface QLConnector extends QLObject {}
