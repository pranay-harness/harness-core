/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.instance.stats.collector;

import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.apache.commons.collections.CollectionUtils;

@Value
@AllArgsConstructor
public class SimplePercentile {
  private List<Integer> data;

  public Integer evaluate(double percentile) {
    List<Integer> sortedData = data.stream().sorted().collect(Collectors.toList());
    if (CollectionUtils.isEmpty(sortedData)) {
      return -1;
    }

    double p = percentile / 100.0;
    int index = (int) Math.floor(p * sortedData.size());
    return sortedData.get(index);
  }
}
