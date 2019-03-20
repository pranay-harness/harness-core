package software.wings.beans.artifact;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityName;
import io.harness.persistence.PersistentIterable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.NameValuePair;
import software.wings.beans.Variable;
import software.wings.beans.config.ArtifactSourceable;
import software.wings.utils.Util;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;

/**
 * ArtifactStream bean class.
 *
 * @author Rishi
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "artifactStreamType")
@Entity(value = "artifactStream")
@Indexes(@Index(options = @IndexOptions(name = "yaml", unique = true),
    fields = { @Field("appId")
               , @Field("serviceId"), @Field("name") }))
@HarnessExportableEntity
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public abstract class ArtifactStream extends Base implements ArtifactSourceable, PersistentIterable {
  public static final String TEMPLATE_UUID_KEY = "templateUuid";

  protected static final String dateFormat = "HHMMSS";

  public static final String NAME_KEY = "name";
  public static final String SERVICE_ID_KEY = "serviceId";
  public static final String ARTIFACT_STREAM_TYPE_KEY = "artifactStreamType";
  public static final String METADATA_ONLY_KEY = "metadataOnly";
  public static final String SETTING_ID_KEY = "settingId";
  public static final String NEXT_ITERATION_KEY = "nextIteration";

  private String artifactStreamType;
  private String sourceName;
  private String settingId;
  @EntityName private String name;
  private boolean autoPopulate;
  @NotEmpty @Indexed private String serviceId;
  @Deprecated private transient boolean autoDownload;
  @Deprecated private transient boolean autoApproveForProduction;
  private boolean metadataOnly;
  private int failedCronAttempts;
  @Indexed private Long nextIteration;
  private String templateUuid;
  private String templateVersion;
  private List<Variable> templateVariables = new ArrayList<>();

  public ArtifactStream(String artifactStreamType) {
    this.artifactStreamType = artifactStreamType;
    this.autoPopulate = true;
  }

  public ArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, String entityYamlPath, String artifactStreamType, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, boolean metadataOnly) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.artifactStreamType = artifactStreamType;
    this.sourceName = sourceName;
    this.settingId = settingId;
    this.name = name;
    this.autoPopulate = autoPopulate;
    this.serviceId = serviceId;
    this.metadataOnly = metadataOnly;
  }

  public String generateName() {
    return Util.normalize(generateSourceName());
  }

  public abstract String generateSourceName();

  public abstract ArtifactStreamAttributes fetchArtifactStreamAttributes();

  public abstract String fetchArtifactDisplayName(String buildNo);

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public abstract static class Yaml extends BaseEntityYaml {
    private String serverName;
    private boolean metadataOnly;
    private String templateUri;
    private List<NameValuePair> templateVariables;

    public Yaml(String type, String harnessApiVersion, String serverName, boolean metadataOnly) {
      super(type, harnessApiVersion);
      this.serverName = serverName;
      this.metadataOnly = metadataOnly;
    }

    public Yaml(String type, String harnessApiVersion) {
      super(type, harnessApiVersion);
    }

    public Yaml(String type, String harnessApiVersion, String serverName, boolean metadataOnly, String templateUri,
        List<NameValuePair> templateVariables) {
      super(type, harnessApiVersion);
      this.serverName = serverName;
      this.metadataOnly = metadataOnly;
      this.templateUri = templateUri;
      this.templateVariables = templateVariables;
    }
  }
}
