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
    final Application application = objects.stream()
                                        .filter(obj -> obj instanceof Application)
                                        .findFirst()
                                        .map(obj -> (Application) obj)
                                        .orElse(null);
    return application;
  }

  public Environment obtainEnvironment() {
    final Environment environment = objects.stream()
                                        .filter(obj -> obj instanceof Environment)
                                        .findFirst()
                                        .map(obj -> (Environment) obj)
                                        .orElse(null);
    return environment;
  }

  public Service obtainService() {
    final Service service =
        objects.stream().filter(obj -> obj instanceof Service).findFirst().map(obj -> (Service) obj).orElse(null);
    return service;
  }
}
