package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.CollectionUtils.nullIfEmpty;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
