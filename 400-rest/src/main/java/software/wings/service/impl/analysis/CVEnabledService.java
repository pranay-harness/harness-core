package software.wings.service.impl.analysis;

import software.wings.beans.Service;
import software.wings.verification.CVConfiguration;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CVEnabledService {
  private Service service;
  List<CVConfiguration> cvConfig;
  private String appName;
  private String appId;
}
