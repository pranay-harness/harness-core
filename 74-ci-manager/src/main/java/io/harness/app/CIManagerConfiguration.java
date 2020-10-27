package io.harness.app;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.stream.Collectors.toSet;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.bundles.assets.AssetsBundleConfiguration;
import io.dropwizard.bundles.assets.AssetsConfiguration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.harness.ci.beans.entities.LogServiceConfig;
import io.harness.mongo.MongoConfig;
import io.harness.remote.client.ServiceHttpClientConfig;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.reflections.Reflections;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.Path;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class CIManagerConfiguration extends Configuration implements AssetsBundleConfiguration {
  public static final String BASE_PACKAGE = "io.harness.app.resources";
  @JsonProperty
  private AssetsConfiguration assetsConfiguration =
      AssetsConfiguration.builder()
          .mimeTypes(of("js", "application/json; charset=UTF-8", "zip", "application/zip"))
          .build();
  @Builder.Default @JsonProperty("cimanager-mongo") private MongoConfig harnessCIMongo = MongoConfig.builder().build();
  @Builder.Default @JsonProperty("harness-mongo") private MongoConfig harnessMongo = MongoConfig.builder().build();
  @JsonProperty("swagger") private SwaggerBundleConfiguration swaggerBundleConfiguration;
  private ScmConnectionConfig scmConnectionConfig;

  @JsonProperty("managerClientConfig") private ServiceHttpClientConfig managerClientConfig;
  @JsonProperty("ngManagerClientConfig") private ServiceHttpClientConfig ngManagerClientConfig;

  private String ngManagerServiceSecret;
  private LogServiceConfig logServiceConfig;

  private String managerServiceSecret;
  private String jwtAuthSecret;
  private boolean enableAuth;
  private String managerTarget;
  private String managerAuthority;

  public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
    SwaggerBundleConfiguration defaultSwaggerBundleConfiguration = new SwaggerBundleConfiguration();

    String resourcePackage = String.join(",", getUniquePackages(getResourceClasses()));
    defaultSwaggerBundleConfiguration.setResourcePackage(resourcePackage);
    defaultSwaggerBundleConfiguration.setSchemes(new String[] {"https", "http"});
    defaultSwaggerBundleConfiguration.setHost(
        "localhost"); // TODO, we should set the appropriate host here ex: qa.harness.io etc
    defaultSwaggerBundleConfiguration.setTitle("CI API Reference");
    defaultSwaggerBundleConfiguration.setVersion("2.0");

    return Optional.ofNullable(swaggerBundleConfiguration).orElse(defaultSwaggerBundleConfiguration);
  }

  @Override
  public AssetsConfiguration getAssetsConfiguration() {
    return assetsConfiguration;
  }

  public static Collection<Class<?>> getResourceClasses() {
    Reflections reflections = new Reflections(BASE_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }

  private static Set<String> getUniquePackages(Collection<Class<?>> classes) {
    return classes.stream().map(aClass -> aClass.getPackage().getName()).collect(toSet());
  }
}
