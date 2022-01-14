package io.harness.persistence;

import io.harness.beans.EmbeddedUser;

public class NoopUserProvider implements UserProvider {
  @Override
  public EmbeddedUser activeUser() {
    return null;
  }
}
