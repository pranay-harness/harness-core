/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.persistence;

import java.util.Date;

public interface ValidUntilAccess {
  String VALID_UNTIL_KEY = "validUntil";

  Date getValidUntil();
}
