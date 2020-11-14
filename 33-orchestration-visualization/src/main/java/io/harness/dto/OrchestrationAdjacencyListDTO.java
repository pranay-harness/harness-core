package io.harness.dto;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EdgeList;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@OwnedBy(CDC)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrchestrationAdjacencyListDTO {
  Map<String, GraphVertexDTO> graphVertexMap;
  Map<String, EdgeList> adjacencyMap;
}
