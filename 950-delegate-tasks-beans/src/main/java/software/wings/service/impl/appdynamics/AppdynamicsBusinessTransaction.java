/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.appdynamics;

import lombok.Data;

/**
 * Created by rsingh on 5/11/17.
 */
@Data
public class AppdynamicsBusinessTransaction {
  private long id;
  private String name;
  private String entryPointType;
  private String internalName;
  private long tierId;
  private String tierName;
  private boolean background;
}
