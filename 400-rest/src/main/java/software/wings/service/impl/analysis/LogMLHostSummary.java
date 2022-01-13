/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * Created by rsingh on 6/30/17.
 */

@Data
public class LogMLHostSummary {
  private int count;
  private double xCordinate;
  private double yCordinate;
  private boolean unexpectedFreq;
  private List<Integer> frequencies;
  private Map<Integer, Integer> frequencyMap;
}
