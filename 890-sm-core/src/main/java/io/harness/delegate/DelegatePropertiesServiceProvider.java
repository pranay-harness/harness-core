/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate;

import io.harness.managerclient.GetDelegatePropertiesRequest;
import io.harness.managerclient.GetDelegatePropertiesResponse;

public interface DelegatePropertiesServiceProvider {
  GetDelegatePropertiesResponse getDelegateProperties(GetDelegatePropertiesRequest request);
}
