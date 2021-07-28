package io.harness.nexus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.CDC)
public class Nexus3Response {
  private int tid;
  private String action;
  private String method;
  private Nexus3Result result;
  private String type;
}
