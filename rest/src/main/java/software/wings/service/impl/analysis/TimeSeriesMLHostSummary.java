package software.wings.service.impl.analysis;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Created by sriram_parthasarathy on 9/24/17.
 */
@Data
public class TimeSeriesMLHostSummary {
  private List<Double> distance;
  private List<Double> control_data;
  private List<Double> test_data;
  private List<Character> control_cuts;
  private List<Character> test_cuts;
  private String optimal_cuts;
  private List<Double> optimal_data;
  private int control_index;
  private int test_index;
  private String nn;
  private double risk;
  private double score;
}
