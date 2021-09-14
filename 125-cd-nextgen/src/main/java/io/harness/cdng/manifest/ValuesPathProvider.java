/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.manifest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

public interface ValuesPathProvider {
  @JsonIgnore List<String> getValuesPathsToFetch();
}
