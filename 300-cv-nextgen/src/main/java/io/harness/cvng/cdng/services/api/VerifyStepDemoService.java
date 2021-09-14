/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.cdng.services.api;

public interface VerifyStepDemoService {
  void createTimeSeriesDemoTemplate(String verificationTaskId);

  void createLogsDemoTemplate(String verificationTaskId);
}
