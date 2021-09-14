/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.datahandler.services;

public interface AdminUserService {
  boolean enableOrDisableUser(String accountId, String userIdOrEmail, boolean enabled);
}
