/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.app;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScmConnectionConfig {
  String url;
}
