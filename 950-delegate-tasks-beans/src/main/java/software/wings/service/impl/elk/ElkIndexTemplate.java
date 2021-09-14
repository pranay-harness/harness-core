/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.elk;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.Data;

/**
 * Created by rsingh on 8/23/17.
 */
@Data
@OwnedBy(HarnessTeam.CV)
public class ElkIndexTemplate {
  private String name;
  private Map<String, Object> properties;
}
