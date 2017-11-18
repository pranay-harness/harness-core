package software.wings.helpers.ext.nexus.model;

import lombok.Builder;

import java.util.List;

/**
 * Created by sgurubelli on 11/17/17.
 */
@lombok.Data
@Builder
public class RequestData {
  private List<Filter> filter;

  @lombok.Data
  @Builder
  public static class Filter {
    private String property;
    private String value;
  }
}
