package io.harness.notifications.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CVAlertFilters {
  private List<String> appIds;
  private List<String> envIds;
  private List<String> cvConfigIds;
  private double alertMinThreshold;
}
