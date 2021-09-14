/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.elk;

import lombok.Data;

/**
 * Created by rsingh on 8/1/17.
 */
@Data
public class ElkAuthenticationError {
  private String type;
  private String reason;
}
