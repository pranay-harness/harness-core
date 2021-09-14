/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.query;

import com.google.common.collect.ImmutableList;
import java.util.List;

public interface PersistentQuery {
  default List<String> queryCanonicalForms() {
    return ImmutableList.<String>builder().build();
  }
}
