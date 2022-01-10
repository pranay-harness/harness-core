/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class TimeGraphResponse {
  @NonNull Long startTime;
  @NonNull Long endTime;
  List<DataPoints> dataPoints;

  @Value
  @Builder
  public static class DataPoints {
    Double value;
    @NonNull Long timeStamp;
  }
}
