package io.harness.presentation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.cache.Distributable;
import io.harness.cache.Ordinal;
import io.harness.execution.status.Status;
import lombok.Builder;
import lombok.Value;

import java.io.ObjectStreamClass;
import java.util.List;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Graph implements Distributable, Ordinal {
  public static final long STRUCTURE_HASH = ObjectStreamClass.lookup(Graph.class).getSerialVersionUID();
  public static final long ALGORITHM_ID = 1;

  // cache variables
  long cacheContextOrder;
  String cacheKey;
  List<String> cacheParams;
  long lastUpdatedAt = System.currentTimeMillis();

  String planExecutionId;
  Long startTs;
  Long endTs;
  Status status;
  GraphVertex graphVertex;

  @Override
  public long structureHash() {
    return STRUCTURE_HASH;
  }

  @Override
  public long algorithmId() {
    return ALGORITHM_ID;
  }

  @Override
  public String key() {
    return cacheKey;
  }

  @Override
  public List<String> parameters() {
    return cacheParams;
  }

  @Override
  public long contextOrder() {
    return cacheContextOrder;
  }
}
