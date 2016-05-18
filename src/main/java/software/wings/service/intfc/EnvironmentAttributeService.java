package software.wings.service.intfc;

import software.wings.beans.EnvironmentAttribute;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * Created by anubhaw on 5/17/16.
 */
public interface EnvironmentAttributeService {
  PageResponse<EnvironmentAttribute> list(PageRequest<EnvironmentAttribute> req);

  EnvironmentAttribute save(EnvironmentAttribute envVar);

  EnvironmentAttribute get(String appId, String envId, String varId);

  EnvironmentAttribute update(EnvironmentAttribute envVar);

  void delete(String appId, String envId, String varId);
}
