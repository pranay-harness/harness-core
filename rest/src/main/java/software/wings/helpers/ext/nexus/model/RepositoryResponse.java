package software.wings.helpers.ext.nexus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by sgurubelli on 11/17/17.
 */
@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepositoryResponse {
  private int tid;
  private String action;
  private String method;
  private Result result;
  private String type;
}