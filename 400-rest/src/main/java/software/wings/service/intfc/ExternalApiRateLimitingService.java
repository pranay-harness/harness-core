/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;

public interface ExternalApiRateLimitingService {
  boolean rateLimitRequest(@NotEmpty String key);
  double getMaxQPMPerManager();
}
