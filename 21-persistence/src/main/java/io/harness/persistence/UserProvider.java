package io.harness.persistence;

import io.harness.beans.EmbeddedUser;

public interface UserProvider {
  EmbeddedUser activeUser();
}
