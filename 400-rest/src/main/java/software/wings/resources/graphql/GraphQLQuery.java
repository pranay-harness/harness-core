/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.resources.graphql;

import java.util.Map;
import lombok.Data;

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
