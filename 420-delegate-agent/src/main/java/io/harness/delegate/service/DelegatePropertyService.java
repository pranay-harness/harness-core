/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.service;

import io.harness.managerclient.GetDelegatePropertiesRequest;
import io.harness.managerclient.GetDelegatePropertiesResponse;

import java.util.concurrent.ExecutionException;

public interface DelegatePropertyService {
  // Query for a specific set of delegate properties for a given account. Results may be
  // up to 15 minutes old.
  GetDelegatePropertiesResponse getDelegateProperties(GetDelegatePropertiesRequest request) throws ExecutionException;

  // If the results are stale now, remove all instances from the cache so that when you
  // query getDelegateProperties again you force a new result
  void resetCache();
}
