/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.CollectionUtils.nullIfEmpty;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class MetadataUtils {
  public <T, U> List<U> map(List<T> input, Function<T, U> fn) {
    if (isEmpty(input)) {
      return null;
    }

    return nullIfEmpty(input.stream().map(fn).filter(Objects::nonNull).collect(Collectors.toList()));
  }

  public <T> List<T> dedup(List<T> input, Function<T, String> fn) {
    if (isNotEmpty(input)) {
      HashSet<String> seen = new HashSet<>();
      input.removeIf(el -> {
        String value = fn.apply(el);
        return value == null || !seen.add(value);
      });
    }

    return nullIfEmpty(input);
  }

  public <T extends GraphNodeVisitable> void acceptMultiple(GraphNodeVisitor visitor, List<T> graphNodes) {
    if (isEmpty(graphNodes)) {
      return;
    }

    for (GraphNodeVisitable nodeMetadata : graphNodes) {
      if (nodeMetadata != null) {
        nodeMetadata.accept(visitor);
      }
    }
  }
}
