package software.wings.service.impl.instance.stats.collector;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

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
