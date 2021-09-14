/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.instance.dashboard;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Service info with parent app info
 * @author rktummala on 08/13/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceSummary extends AbstractEntitySummary {
  private EntitySummary appSummary;

  @Builder
  public ServiceSummary(String id, String name, String type, EntitySummary appSummary) {
    super(id, name, type);
    this.appSummary = appSummary;
  }
}
