package io.harness.app.beans.entities;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthorInfo {
  private String name;
  private String url;
}
