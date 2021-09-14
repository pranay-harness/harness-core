/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector;

import java.util.Set;

public interface DelegateSelectable {
  Set<String> getDelegateSelectors();
  void setDelegateSelectors(Set<String> delegateSelectors);
}
