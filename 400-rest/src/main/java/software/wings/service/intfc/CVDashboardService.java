package software.wings.service.intfc;

import software.wings.verification.HeatMap;

import java.util.List;

/**
 * @author Vaibhav Tulsyan
 * 11/Oct/2018
 */
public interface CVDashboardService {
  List<HeatMap> getHeatMap(String accountId, String appId, int resolution, String startTime, String endTime);
}
