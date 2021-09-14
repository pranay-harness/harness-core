/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.cdng.services.api;

import io.harness.cvng.cdng.entities.CVNGStepTask;

public interface CVNGStepTaskService {
  void create(CVNGStepTask cvngStepTask);
  void notifyCVNGStep(CVNGStepTask entity);
  CVNGStepTask getByCallBackId(String callBackId);
}
