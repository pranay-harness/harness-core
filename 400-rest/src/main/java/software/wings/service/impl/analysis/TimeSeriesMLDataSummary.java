/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import java.util.List;
import lombok.Data;

/**
 * Created by sriram_parthasarathy on 9/22/17.
 */
@Data
public class TimeSeriesMLDataSummary {
  private List<List<Double>> data;
  private List<List<Double>> weights;
  private String weights_type;
  private String data_type;
  private List<String> host_names;
}
