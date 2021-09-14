/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
