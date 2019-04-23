package io.harness.notifications.beans;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CVAlertFilters {
  private List<String> appIds;
  private List<String> envIds;
  private List<String> cvConfigIds;
  private double alertMinThreshold;
  private double alertMaxThreshold;
}
