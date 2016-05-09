package software.wings.service.intfc;

import software.wings.beans.Environment;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;

/**
 * Created by anubhaw on 4/1/16.
 */
public interface EnvironmentService {
  public PageResponse<Environment> list(PageRequest<Environment> request);

  public Environment get(String envName);

  public Environment save(Environment environment);

  public Environment update(Environment environment);

  public void delete(String envId);
}
