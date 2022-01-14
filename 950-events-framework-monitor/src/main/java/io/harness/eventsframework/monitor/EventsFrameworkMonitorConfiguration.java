package io.harness.eventsframework.monitor;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import java.util.Collection;
import javax.ws.rs.Path;
import lombok.Getter;
import org.reflections.Reflections;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.PL)
public class EventsFrameworkMonitorConfiguration extends Configuration {
  public static final String SERVICE_ID = "events-framework-monitor";
  public static final String BASE_PACKAGE = "io.harness.eventsframework.monitor";
  public static final String RESOURCE_PACKAGE = "io.harness.eventsframework.monitor.resources";
  @JsonProperty("eventsFramework") private EventsFrameworkConfiguration eventsFrameworkConfiguration;
  @JsonProperty("stackDriverMetricsPushEnabled") private boolean stackDriverMetricsPushEnabled;

  public static Collection<Class<?>> getResourceClasses() {
    Reflections reflections = new Reflections(RESOURCE_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }
}