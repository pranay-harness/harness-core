package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
// TODO: this class seems pointless copy of EmbeddedUser
public class EmbeddedUserDetails {
  private String uuid;
  private String name;
  private String email;
}
