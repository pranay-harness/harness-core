/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.terraform;

import java.util.concurrent.TimeUnit;

public class TerraformConstants {
  static final long DEFAULT_TERRAFORM_COMMAND_TIMEOUT = TimeUnit.MINUTES.toMillis(30);
}
