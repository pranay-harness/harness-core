package io.harness.nexus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

/**
 * Created by sgurubelli on 11/17/17.
 */
@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.CDC)
public class RepositoryResponse {
  private int tid;
  private String action;
  private String method;
  private Result result;
  private String type;
}
