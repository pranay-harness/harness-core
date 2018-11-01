package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.Service;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class CVEnabledService {
  Service service;
  List<String> cvConfigurations;
  String appName;
}
