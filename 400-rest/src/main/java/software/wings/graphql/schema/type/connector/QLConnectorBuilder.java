package software.wings.graphql.schema.type.connector;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLUser;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface QLConnectorBuilder {
  QLConnectorBuilder id(String id);
  QLConnectorBuilder name(String name);
  QLConnectorBuilder createdAt(Long createdAt);
  QLConnectorBuilder createdBy(QLUser createdBy);
  QLConnector build();
}
