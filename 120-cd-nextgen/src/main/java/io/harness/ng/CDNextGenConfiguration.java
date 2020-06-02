package io.harness.ng;

import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.harness.mongo.MongoConfig;
import lombok.Getter;
import org.reflections.Reflections;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.Path;

@Getter
public class CDNextGenConfiguration extends Configuration {
  public static final String BASE_PACKAGE = "io.harness.ng";
  @JsonProperty("swagger") private SwaggerBundleConfiguration swaggerBundleConfiguration;
  @JsonProperty("mongo") private MongoConfig mongoConfig;
  @JsonProperty("allowedOrigins") private List<String> allowedOrigins = Lists.newArrayList();

  public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
    SwaggerBundleConfiguration defaultSwaggerBundleConfiguration = new SwaggerBundleConfiguration();

    String resourcePackage = String.join(",", getUniquePackages(getResourceClasses()));
    defaultSwaggerBundleConfiguration.setResourcePackage(resourcePackage);
    defaultSwaggerBundleConfiguration.setSchemes(new String[] {"https", "http"});
    defaultSwaggerBundleConfiguration.setHost("{{host}}");

    return Optional.ofNullable(swaggerBundleConfiguration).orElse(defaultSwaggerBundleConfiguration);
  }

  public static Collection<Class<?>> getResourceClasses() {
    Reflections reflections = new Reflections(BASE_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }

  private static Set<String> getUniquePackages(Collection<Class<?>> classes) {
    return classes.stream().map(aClass -> aClass.getPackage().getName()).collect(toSet());
  }
}