/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.dto.common;

import io.leangen.graphql.annotations.GraphQLNonNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataPoint {
  @GraphQLNonNull Reference key;
  @GraphQLNonNull Number value;
}
