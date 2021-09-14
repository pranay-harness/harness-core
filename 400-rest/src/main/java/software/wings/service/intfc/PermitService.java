/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import software.wings.beans.Permit;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface PermitService {
  String acquirePermit(@Valid Permit permit);
  boolean releasePermitByKey(@NotNull String key);
}
