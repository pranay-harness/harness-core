package software.wings.beans;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bzane on 7/20/17.
 */
public class DelegateTaskProperties {
  // env, ip subnet, task type, service type
  private List<String> environments = new ArrayList<>();

  public List<String> getEnvironments() {
    return environments;
  }

  public void addEnvironment(String envId) {
    environments.add(envId);
  }

  public void removeEnvironment(String envId) {
    environments.remove(envId);
  }

  public void setEnvironments(List<String> environments) {
    this.environments = environments;
  }
}
