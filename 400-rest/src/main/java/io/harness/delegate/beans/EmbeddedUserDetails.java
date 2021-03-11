package io.harness.delegate.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._420_DELEGATE_SERVICE)
// TODO: this class seems pointless copy of EmbeddedUser
public class EmbeddedUserDetails {
  private String uuid;
  private String name;
  private String email;
}
