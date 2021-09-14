/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.dto;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.Getter;

/**
 * At present this is a representation of
 * query that comes from GraphiQL IDE or Postman App
 */

@Getter
@OwnedBy(CE)
public class GraphQLQuery {
  String operationName;
  String query;
  Map<String, Object> variables;
}
