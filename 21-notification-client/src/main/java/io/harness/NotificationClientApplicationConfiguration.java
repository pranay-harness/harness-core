package io.harness;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import io.dropwizard.Configuration;
import io.harness.mongo.MongoConfig;
import io.harness.notification.NotificationClientConfiguration;
import lombok.Getter;
import org.reflections.Reflections;

import javax.ws.rs.Path;
import java.util.Collection;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationClientApplicationConfiguration extends Configuration {
  public static final String SERVICE_ID = "notification-microservice";
  public static final String BASE_PACKAGE = "io.harness.notification";
  public static final String RESOURCE_PACKAGE = "io.harness.notification.resources";
  @JsonProperty("mongo") private MongoConfig mongoConfig;
  @JsonProperty("allowedOrigins") private List<String> allowedOrigins = Lists.newArrayList();
  @JsonProperty("notificationClient") private NotificationClientConfiguration notificationClientConfiguration;
  @JsonProperty(value = "enableAuth", defaultValue = "true") private boolean enableAuth;
  @JsonProperty(value = "environment", defaultValue = "dev") private String environment;

  public static Collection<Class<?>> getResourceClasses() {
    Reflections reflections = new Reflections(RESOURCE_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }
}
