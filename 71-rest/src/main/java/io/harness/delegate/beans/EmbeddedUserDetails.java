package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmbeddedUserDetails {
  private String uuid;
  private String name;
  private String email;
}
