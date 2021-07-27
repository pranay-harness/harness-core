package io.harness.nexus.model;

import java.util.List;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;

/**
 * Created by sgurubelli on 11/17/17.
 */
@lombok.Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class RequestData {
  private List<Filter> filter;

  @lombok.Data
  @Builder
  public static class Filter {
    private String property;
    private String value;
  }
}
