/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.app.schema.query.delegate;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.app.schema.type.delegate.QLDelegateStatus;
import io.harness.app.schema.type.delegate.QLDelegateType;

import software.wings.graphql.schema.type.aggregation.EntityFilter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(DEL)
public class QLDelegateFilter implements EntityFilter {
  String accountId;
  QLDelegateType delegateType;
  QLDelegateStatus delegateStatus;
  String delegateName;
}
