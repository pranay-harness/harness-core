/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.reconciliation.service;

import io.harness.manage.ManagedScheduledExecutorService;

import com.google.inject.Singleton;

@Singleton
public class DeploymentReconExecutorService extends ManagedScheduledExecutorService {
  public DeploymentReconExecutorService() {
    super("DeploymentRecon");
  }
}
