package software.wings.helpers.ext.nexus.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@lombok.Data
@Builder
public class Nexus3Request {
  private String action;
  private String method;
  @JsonProperty("data") private List<Nexus3RequestData> data;
  private String type;
  private int tid;
}
