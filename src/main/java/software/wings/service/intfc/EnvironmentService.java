package software.wings.service.intfc;

import software.wings.beans.Environment;

import java.util.List;

/**
 * Created by anubhaw on 4/1/16.
 */
public interface EnvironmentService {
  public List<Environment> list(String appId);

  public Environment get(String appId, String envName);

  public Environment save(String appId, Environment environment);
}
