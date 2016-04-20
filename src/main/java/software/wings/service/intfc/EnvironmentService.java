package software.wings.service.intfc;

import software.wings.beans.Environment;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;

import java.util.List;

/**
 * Created by anubhaw on 4/1/16.
 */
public interface EnvironmentService {
  public List<Environment> listEnvironments(String appID);

  public Environment getEnvironments(String applicationId, String envName);

  public Environment createEnvironment(String applicationId, Environment environment);
}
