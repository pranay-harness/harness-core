package software.wings.service.impl.analysis;

import lombok.Data;

import java.util.List;
import java.util.Map;

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
