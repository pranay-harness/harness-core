/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Vaibhav Tulsyan
 * 24/Oct/2018
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesRisk {
  private long startTime;
  private long endTime;
  private int risk;
}
