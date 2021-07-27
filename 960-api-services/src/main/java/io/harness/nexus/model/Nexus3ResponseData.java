package io.harness.nexus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.CDC)
public class Nexus3ResponseData {
  private String assetId;
  private String componentId;
  private String id;
  private String leaf;
  private String text;
  private String type;
}
