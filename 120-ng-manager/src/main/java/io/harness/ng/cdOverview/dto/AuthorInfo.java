package io.harness.ng.cdOverview.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthorInfo {
  private String name;
  private String url;
}
