package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.MongoConfig;
import io.harness.timescaledb.TimeScaleDBConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Singleton;
import io.dropwizard.Configuration;
import java.util.Collection;
import javax.ws.rs.Path;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.reflections.Reflections;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
@OwnedBy(HarnessTeam.CE)
public class ChangeDataCaptureServiceConfig extends Configuration {
  public static final String RESOURCE_PACKAGE = "io.harness.resources";

  @JsonProperty("harness-mongo") private MongoConfig harnessMongo = MongoConfig.builder().build();
  @JsonProperty("events-mongo") private MongoConfig eventsMongo = MongoConfig.builder().build();
  @JsonProperty("cdc-mongo") private MongoConfig cdcMongo = MongoConfig.builder().build();
  @JsonProperty("pms-harness") private MongoConfig pmsMongo = MongoConfig.builder().build();
  @JsonProperty("timescaledb") private TimeScaleDBConfig timeScaleDBConfig;
  @JsonProperty("mongotags") private MongoTagsConfig mongoTagsConfig = MongoTagsConfig.builder().build();
  @JsonProperty("gcp-project-id") private String gcpProjectId;

  public static Collection<Class<?>> getResourceClasses() {
    Reflections reflections = new Reflections(RESOURCE_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }
}
