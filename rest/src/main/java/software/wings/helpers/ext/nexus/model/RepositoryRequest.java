package software.wings.helpers.ext.nexus.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

/**
 * Created by sgurubelli on 11/17/17.
 */
@lombok.Data
@Builder
public class RepositoryRequest {
  private String action;
  private String method;
  @JsonProperty("data") private List<RequestData> data;
  private String type;
  private int tid;
}