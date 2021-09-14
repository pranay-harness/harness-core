/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo.index.migrator;

import lombok.Data;

@Data
public class AccountAndName {
  private String accountId;
  private String name;
}
