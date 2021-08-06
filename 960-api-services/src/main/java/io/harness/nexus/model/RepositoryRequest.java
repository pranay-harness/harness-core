package io.harness.nexus.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

/**
 * Created by sgurubelli on 11/17/17.
 */
@lombok.Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class RepositoryRequest {
  private String action;
  private String method;
  @JsonProperty("data") private List<RequestData> data;
  private String type;
  private int tid;
}
