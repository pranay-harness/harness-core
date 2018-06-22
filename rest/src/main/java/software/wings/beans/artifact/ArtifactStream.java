package software.wings.beans.artifact;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.data.validator.EntityName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.config.ArtifactSourceable;
import software.wings.utils.Util;
import software.wings.yaml.BaseEntityYaml;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * ArtifactStream bean class.
 *
 * @author Rishi
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "artifactStreamType")
@Entity(value = "artifactStream")
@Indexes(
    @Index(fields = { @Field("appId")
                      , @Field("serviceId"), @Field("name") }, options = @IndexOptions(unique = true)))
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public abstract class ArtifactStream extends Base implements ArtifactSourceable {
  protected static final DateFormat dateFormat = new SimpleDateFormat("HHMMSS");

  public static final String SERVICE_ID_KEY = "serviceId";
  public static final String ARTIFACT_STREAM_TYPE_KEY = "artifactStreamType";
  public static final String METADATA_ONLY_KEY = "metadataOnly";
  public static final String SETTING_ID_KEY = "settingId";

  private String artifactStreamType;
  private String sourceName;
  private String settingId;
  @EntityName private String name;
  private boolean autoPopulate;
  @Indexed private String serviceId;
  transient @Deprecated private boolean autoDownload;
  transient @Deprecated private boolean autoApproveForProduction;
  private boolean metadataOnly;

  public ArtifactStream(String artifactStreamType) {
    this.artifactStreamType = artifactStreamType;
    this.autoPopulate = true;
  }

  public ArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath, String artifactStreamType, String sourceName,
      String settingId, String name, boolean autoPopulate, String serviceId, boolean metadataOnly) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
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

  public abstract ArtifactStreamAttributes getArtifactStreamAttributes();

  public abstract String getArtifactDisplayName(String buildNo);

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public abstract static class Yaml extends BaseEntityYaml {
    private String serverName;
    private boolean metadataOnly;

    public Yaml(String type, String harnessApiVersion, String serverName, boolean metadataOnly) {
      super(type, harnessApiVersion);
      this.serverName = serverName;
      this.metadataOnly = metadataOnly;
    }
  }
}
