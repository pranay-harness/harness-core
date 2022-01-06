/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.service.impl.appdynamics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by rsingh on 5/15/17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(of = {"id"})
public class AppdynamicsNode implements Comparable<AppdynamicsNode> {
  private long id;
  private String name;
  private String type;
  private long tierId;
  private String tierName;
  private long machineId;

  @Override
  public int compareTo(AppdynamicsNode o) {
    return name.compareTo(o.getName());
  }
}
