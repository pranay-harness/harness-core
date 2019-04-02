package software.wings.resources.graphql;

import lombok.Data;

import java.util.Map;

/**
 * At present this is a representation of
 * query that comes from GraphiQL IDE
 */
@Data
public class GraphQLQuery {
  String operationName;
  String query;
  Map<String, Object> variables;
}
