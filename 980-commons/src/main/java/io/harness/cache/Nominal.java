/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cache;

public interface Nominal {
  // Returns a hash of the context in which the object was prepared.
  long contextHash();
}
