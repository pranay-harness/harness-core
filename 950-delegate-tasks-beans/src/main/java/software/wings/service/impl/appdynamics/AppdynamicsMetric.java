/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
