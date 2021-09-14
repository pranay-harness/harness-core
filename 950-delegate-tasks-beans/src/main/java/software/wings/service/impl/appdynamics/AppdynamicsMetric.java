/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.appdynamics;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

/**
 * Created by rsingh on 4/19/17.
 */
@Data
@Builder
public class AppdynamicsMetric {
  private AppdynamicsMetricType type;
  private String name;
  @Default private List<AppdynamicsMetric> childMetrices = new ArrayList<>();

  public enum AppdynamicsMetricType { leaf, folder }
}
