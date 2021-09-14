/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.persistence;

import io.harness.beans.EmbeddedUser;

public interface UserProvider {
  EmbeddedUser activeUser();
}
