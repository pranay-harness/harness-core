package software.wings.graphql.schema.type.connector;

import software.wings.graphql.schema.type.QLUser;

import java.time.OffsetDateTime;

public interface QLConnectorBuilder {
  QLConnectorBuilder id(String id);
  QLConnectorBuilder name(String name);
  QLConnectorBuilder createdAt(OffsetDateTime createdAt);
  QLConnectorBuilder createdBy(QLUser createdBy);
}
