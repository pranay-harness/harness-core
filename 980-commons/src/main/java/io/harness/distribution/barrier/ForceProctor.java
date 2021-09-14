/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.distribution.barrier;

import java.util.Map;

public interface ForceProctor {
  Forcer.State getForcerState(ForcerId forcerId, Map<String, Object> metadata);
}
