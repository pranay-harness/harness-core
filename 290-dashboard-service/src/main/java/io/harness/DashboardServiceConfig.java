package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.dropwizard.Configuration;
import java.util.Collection;
import javax.ws.rs.Path;
import lombok.Getter;
import org.reflections.Reflections;

@Getter
@OwnedBy(HarnessTeam.PL)
public class DashboardServiceConfig extends Configuration {
  public static Collection<Class<?>> getResourceClasses() {
    // todo @Deepak: Add the packages here in the reflection
    Reflections reflections = new Reflections();
    return reflections.getTypesAnnotatedWith(Path.class);
  }
}
