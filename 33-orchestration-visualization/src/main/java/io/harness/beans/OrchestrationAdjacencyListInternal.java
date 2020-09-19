package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cache.Distributable;
import io.harness.cache.Ordinal;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.io.ObjectStreamClass;
import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrchestrationAdjacencyListInternal implements Distributable, Ordinal {
  public static final long STRUCTURE_HASH =
      ObjectStreamClass.lookup(OrchestrationAdjacencyListInternal.class).getSerialVersionUID();
  public static final long ALGORITHM_ID = 2;

  // cache variables
  @Wither long cacheContextOrder;
  @Wither String cacheKey;
  @Wither List<String> cacheParams;
  long lastUpdatedAt = System.currentTimeMillis();

  Map<String, GraphVertex> graphVertexMap;
  Map<String, EdgeList> adjacencyList;

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
