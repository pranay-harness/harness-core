package software.wings.graphql.schema.type.connector;

import software.wings.graphql.schema.type.QLUser;

public interface QLConnectorBuilder {
  QLConnectorBuilder id(String id);
  QLConnectorBuilder name(String name);
  QLConnectorBuilder createdAt(Long createdAt);
  QLConnectorBuilder createdBy(QLUser createdBy);
  QLConnector build();
}
