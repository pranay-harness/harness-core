package io.harness.nexus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Nexus3ResponseData {
  private String assetId;
  private String componentId;
  private String id;
  private String leaf;
  private String text;
  private String type;
}
