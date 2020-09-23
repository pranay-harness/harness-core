package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cache.Distributable;
import io.harness.cache.Nominal;
import io.harness.execution.status.Status;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;

import java.io.ObjectStreamClass;
import java.util.List;

@OwnedBy(CDC)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrchestrationGraphInternal implements Distributable, Nominal {
  public static final long STRUCTURE_HASH =
      ObjectStreamClass.lookup(OrchestrationGraphInternal.class).getSerialVersionUID();
  public static final long ALGORITHM_ID = 2;

  // cache variables
  long cacheContextOrder;
  String cacheKey;
  List<String> cacheParams;
  long lastUpdatedAt = System.currentTimeMillis();

  String planExecutionId;
  Long startTs;
  @Wither Long endTs;
  @Wither Status status;

  @Wither String rootNodeId;
  OrchestrationAdjacencyList adjacencyList;

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
  public long contextHash() {
    return cacheContextOrder;
  }
}
