/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
