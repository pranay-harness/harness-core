package io.harness.serviceinfo;

import java.util.List;

public interface ServiceInfoService {
  boolean updateLatest(String serviceId, String version);

  List<ServiceInfo> getAllServices();

  List<String> getAllVersions(String service);
}
