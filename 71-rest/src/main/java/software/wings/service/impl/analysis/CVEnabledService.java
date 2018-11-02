package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.Service;
import software.wings.verification.CVConfiguration;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class CVEnabledService {
  private Service service;
  String envName;
  String envId;
  List<CVConfiguration> cvConfig;
  private String appName;
}
