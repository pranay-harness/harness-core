/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ce.recommendation.entity;

import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.EmptyPredicate.IsEmpty;

import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class ResourceRequirement implements IsEmpty {
  public static final String MEMORY = "memory";
  public static final String CPU = "cpu";
  @Singular Map<String, String> requests;
  @Singular Map<String, String> limits;

  @Override
  public boolean isEmpty() {
    return EmptyPredicate.isEmpty(requests) && EmptyPredicate.isEmpty(limits);
  }
}
