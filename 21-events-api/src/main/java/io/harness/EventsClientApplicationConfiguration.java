package io.harness;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import lombok.Getter;
import org.reflections.Reflections;

import javax.ws.rs.Path;
import java.util.Collection;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventsClientApplicationConfiguration extends Configuration {
  public static final String SERVICE_ID = "events-framework-client";
  public static final String BASE_PACKAGE = "io.harness.eventsframework";
  public static final String RESOURCE_PACKAGE = "io.harness.eventsframework.resources";
  @JsonProperty("eventsFramework") private EventsFrameworkConfiguration eventsFrameworkConfiguration;

  public static Collection<Class<?>> getResourceClasses() {
    Reflections reflections = new Reflections(RESOURCE_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }
}
