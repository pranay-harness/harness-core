package software.wings.beans;

import static java.util.Arrays.asList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mongodb.client.model.CollationStrength;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Collation;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.annotations.Version;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.utils.ArtifactType;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;

/**
 * Component bean class.
 *
 * @author Rishi
 */
@Entity(value = "services", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Indexes(value =
    {
      @Index(options = @IndexOptions(name = "yaml", unique = true), fields = { @Field("appId")
                                                                               , @Field("name") })
      ,
          @Index(options = @IndexOptions(
                     name = "collation", collation = @Collation(locale = "en", strength = CollationStrength.PRIMARY)),
              fields = { @Field("name") })
    })

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Service extends Base {
  public static final String NAME_KEY = "name";
  public static final String ARTIFACT_TYPE = "artifactType";

  @Trimmed @EntityName private String name;
  private String description;
  private ArtifactType artifactType;
  private String configMapYaml;
  private String helmValueYaml;

  @Version private long version;

  @Reference(idOnly = true, ignoreMissing = true) private AppContainer appContainer;

  @Transient private List<ConfigFile> configFiles = new ArrayList<>();
  @Transient private List<ServiceVariable> serviceVariables = new ArrayList<>();
  @Transient private List<ArtifactStream> artifactStreams = new ArrayList<>();
  @Transient private List<ServiceCommand> serviceCommands = new ArrayList<>();

  @Transient private Activity lastDeploymentActivity;
  @Transient private Activity lastProdDeploymentActivity;
  @Transient private Setup setup;

  @Builder
  public Service(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath, String name, String description,
      ArtifactType artifactType, String configMapYaml, String helmValueYaml, long version, AppContainer appContainer,
      List<ConfigFile> configFiles, List<ServiceVariable> serviceVariables, List<ArtifactStream> artifactStreams,
      List<ServiceCommand> serviceCommands, Activity lastDeploymentActivity, Activity lastProdDeploymentActivity,
      Setup setup) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.name = name;
    this.description = description;
    this.artifactType = artifactType;
    this.configMapYaml = configMapYaml;
    this.helmValueYaml = helmValueYaml;
    this.version = version;
    this.appContainer = appContainer;
    this.configFiles = configFiles == null ? new ArrayList<>() : configFiles;
    this.serviceVariables = serviceVariables == null ? new ArrayList<>() : serviceVariables;
    this.artifactStreams = artifactStreams == null ? new ArrayList<>() : artifactStreams;
    this.serviceCommands = serviceCommands == null ? new ArrayList<>() : serviceCommands;
    this.lastDeploymentActivity = lastDeploymentActivity;
    this.lastProdDeploymentActivity = lastProdDeploymentActivity;
    this.setup = setup;
  }

  public Service cloneInternal() {
    return Service.builder()
        .appId(getAppId())
        .name(name)
        .description(description)
        .artifactType(artifactType)
        .configMapYaml(configMapYaml)
        .helmValueYaml(helmValueYaml)
        .appContainer(appContainer)
        .build();
  }

  @Override
  public List<Object> generateKeywords() {
    List<Object> keywords = new ArrayList<>();
    keywords.addAll(asList(name, description, artifactType));
    keywords.addAll(super.generateKeywords());
    return keywords;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseEntityYaml {
    private String description;
    private String artifactType;
    private String configMapYaml;
    private String helmValueYaml;
    private String applicationStack;
    private List<NameValuePair.Yaml> configVariables = new ArrayList<>();

    @lombok.Builder
    public Yaml(String harnessApiVersion, String description, String artifactType, String configMapYaml,
        String helmValueYaml, String applicationStack, List<NameValuePair.Yaml> configVariables) {
      super(EntityType.SERVICE.name(), harnessApiVersion);
      this.description = description;
      this.artifactType = artifactType;
      this.configMapYaml = configMapYaml;
      this.helmValueYaml = helmValueYaml;
      this.applicationStack = applicationStack;
      this.configVariables = configVariables;
    }
  }
}
