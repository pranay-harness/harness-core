package io.harness.ng;

import static java.util.stream.Collectors.toSet;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.mongo.MongoConfig;
import io.harness.ng.core.NextGenConfig;
import io.harness.ng.core.SecretManagerClientConfig;
import lombok.Getter;
import org.reflections.Reflections;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.Path;

@Getter
public class NextGenConfiguration extends Configuration {
  public static final String BASE_PACKAGE = "io.harness.ng";
  public static final String CONNECTOR_PACKAGE = "io.harness.connector.apis.resource";
  @JsonProperty("swagger") private SwaggerBundleConfiguration swaggerBundleConfiguration;
  @JsonProperty("mongo") private MongoConfig mongoConfig;
  @JsonProperty("allowedOrigins") private List<String> allowedOrigins = Lists.newArrayList();
  @JsonProperty("secretManagerClient") private SecretManagerClientConfig secretManagerClientConfig;
  @JsonProperty("grpcClient") private GrpcClientConfig grpcClientConfig;
  @JsonProperty("grpcServer") private GrpcServerConfig grpcServerConfig;
  @JsonProperty("nextGen") private NextGenConfig nextGenConfig;

  // [secondary-db]: Uncomment this and the corresponding config in yaml file if you want to connect to another database
  //  @JsonProperty("secondary-mongo") MongoConfig secondaryMongoConfig;

  public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
    SwaggerBundleConfiguration defaultSwaggerBundleConfiguration = new SwaggerBundleConfiguration();

    String resourcePackage = String.join(",", getUniquePackages(getResourceClasses()));
    defaultSwaggerBundleConfiguration.setResourcePackage(resourcePackage);
    defaultSwaggerBundleConfiguration.setSchemes(new String[] {"https", "http"});
    defaultSwaggerBundleConfiguration.setHost(
        "localhost"); // TODO, we should set the appropriate host here ex: qa.harness.io etc
    defaultSwaggerBundleConfiguration.setTitle("CD NextGen API Reference");
    defaultSwaggerBundleConfiguration.setVersion("2.0");

    return Optional.ofNullable(swaggerBundleConfiguration).orElse(defaultSwaggerBundleConfiguration);
  }

  public static Collection<Class<?>> getResourceClasses() {
    Reflections reflections = new Reflections(BASE_PACKAGE, CONNECTOR_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }

  private static Set<String> getUniquePackages(Collection<Class<?>> classes) {
    return classes.stream().map(aClass -> aClass.getPackage().getName()).collect(toSet());
  }
}