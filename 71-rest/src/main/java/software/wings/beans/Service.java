package software.wings.beans;

import static java.util.Arrays.asList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.NameAccess;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.annotations.Version;
import software.wings.api.DeploymentType;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamBinding;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.entityinterface.KeywordsAware;
import software.wings.beans.entityinterface.TagAware;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;
import software.wings.utils.ArtifactType;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Component bean class.
 *
 * @author Rishi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Indexes(@Index(options = @IndexOptions(name = "yaml", unique = true), fields = { @Field("appId")
                                                                                  , @Field("name") }))
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "ServiceKeys")
@Entity(value = "services", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class Service extends Base implements KeywordsAware, NameAccess, TagAware, AccountAccess {
  public static final String GLOBAL_SERVICE_NAME_FOR_YAML = "__all_service__";
  @Trimmed(message = "Service Name should not contain leading and trailing spaces")
  @EntityName
  @NotEmpty
  private String name;
  private String description;
  private ArtifactType artifactType;
  private DeploymentType deploymentType;
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

  @SchemaIgnore private Set<String> keywords;

  private boolean isK8sV2;
  private boolean isPcfV2;
  private HelmVersion helmVersion;
  @Indexed private String accountId;
  @Indexed private List<String> artifactStreamIds;
  @Transient private List<ArtifactStreamBinding> artifactStreamBindings;
  private boolean sample;

  private transient List<HarnessTagLink> tagLinks;

  @Builder
  public Service(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, Set<String> keywords, String entityYamlPath, String name, String description,
      ArtifactType artifactType, DeploymentType deploymentType, String configMapYaml, String helmValueYaml,
      long version, AppContainer appContainer, List<ConfigFile> configFiles, List<ServiceVariable> serviceVariables,
      List<ArtifactStream> artifactStreams, List<ServiceCommand> serviceCommands, Activity lastDeploymentActivity,
      Activity lastProdDeploymentActivity, Setup setup, boolean isK8sV2, String accountId,
      List<String> artifactStreamIds, boolean sample, boolean isPcfV2, HelmVersion helmVersion) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.name = name;
    this.description = description;
    this.artifactType = artifactType;
    this.deploymentType = deploymentType;
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
    this.keywords = keywords;
    this.isK8sV2 = isK8sV2;
    this.isPcfV2 = isPcfV2;
    this.accountId = accountId;
    this.artifactStreamIds = artifactStreamIds;
    this.sample = sample;
    this.helmVersion = helmVersion;
  }

  // TODO: check what to do with artifactStreamIds and artifactStreamBindings
  public Service cloneInternal() {
    return Service.builder()
        .appId(getAppId())
        .accountId(getAccountId())
        .name(name)
        .description(description)
        .artifactType(artifactType)
        .deploymentType(deploymentType)
        .configMapYaml(configMapYaml)
        .helmValueYaml(helmValueYaml)
        .appContainer(appContainer)
        .isK8sV2(isK8sV2)
        .isPcfV2(isPcfV2)
        .helmVersion(helmVersion)
        .build();
  }

  @Override
  public Set<String> generateKeywords() {
    Set<String> keywords = KeywordsAware.super.generateKeywords();
    keywords.addAll(asList(name, description));
    if (artifactType != null) {
      keywords.add(artifactType.name());
    }
    return keywords;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends BaseEntityYaml {
    private String description;
    private String artifactType;
    private String deploymentType;
    private String configMapYaml;
    private String applicationStack;
    private String helmVersion;
    private List<NameValuePair.Yaml> configVariables = new ArrayList<>();

    @lombok.Builder
    public Yaml(String harnessApiVersion, String description, String artifactType, String deploymentType,
        String configMapYaml, String applicationStack, List<NameValuePair.Yaml> configVariables, String helmVersion) {
      super(EntityType.SERVICE.name(), harnessApiVersion);
      this.description = description;
      this.artifactType = artifactType;
      this.deploymentType = deploymentType;
      this.configMapYaml = configMapYaml;
      this.applicationStack = applicationStack;
      this.configVariables = configVariables;
      this.helmVersion = helmVersion;
    }
  }

  @UtilityClass
  public static final class ServiceKeys {
    // Temporary
    public static final String appId = "appId";
    public static final String createdAt = "createdAt";
    public static final String uuid = "uuid";
  }
}
