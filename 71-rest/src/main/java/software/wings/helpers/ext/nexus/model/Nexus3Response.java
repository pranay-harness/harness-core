package software.wings.helpers.ext.nexus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Nexus3Response {
  private int tid;
  private String action;
  private String method;
  private Nexus3Result result;
  private String type;
}
