package software.wings.generator;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;

import java.util.ArrayList;
import java.util.List;

public class Owners {
  private List<Object> objects = new ArrayList();

  public void add(Object owner) {
    objects.add(owner);
  }

  public Application obtainApplication() {
    return objects.stream()
        .filter(obj -> obj instanceof Application)
        .findFirst()
        .map(obj -> (Application) obj)
        .orElse(null);
  }

  public Environment obtainEnvironment() {
    return objects.stream()
        .filter(obj -> obj instanceof Environment)
        .findFirst()
        .map(obj -> (Environment) obj)
        .orElse(null);
  }

  public Service obtainService() {
    return objects.stream().filter(obj -> obj instanceof Service).findFirst().map(obj -> (Service) obj).orElse(null);
  }
}
