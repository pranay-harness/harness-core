package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.dto.Principal;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class PrincipalThreadLocal {
  public static class Guard implements AutoCloseable {
    private Principal old;
    Guard(Principal principal) {
      old = get();
      set(principal);
    }
    @Override
    public void close() {
      set(old);
    }
  }
  public final ThreadLocal<Principal> principalThreadLocal = new ThreadLocal<>();

  public void set(Principal principal) {
    principalThreadLocal.set(principal);
  }

  public void unset() {
    principalThreadLocal.remove();
  }

  public Principal get() {
    return principalThreadLocal.get();
  }
}