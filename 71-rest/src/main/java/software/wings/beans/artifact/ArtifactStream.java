package software.wings.beans.artifact;

import static java.util.Arrays.asList;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityName;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.NameAccess;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.config.ArtifactSourceable;
import software.wings.beans.entityinterface.KeywordsAware;
import software.wings.utils.Utils;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ArtifactStream bean class.
 *
 * @author Rishi
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "artifactStreamType")
@Indexes({
  @Index(options = @IndexOptions(name = "yaml", unique = true),
      fields = { @Field("appId")
                 , @Field("serviceId"), @Field("name") })
  ,
      @Index(options = @IndexOptions(name = "artifactStream_cleanup"), fields = {
        @Field("artifactStreamType"), @Field("nextCleanupIteration")
      })
})
// TODO: ASR: add compound index with setting_id + name
// TODO: ASR: change all apis to work with Service.artifactStreamIds instead of serviceId - including UI
// TODO: ASR: migrate the index and name of existing artifact streams (name + service name + app name + setting name)
// TODO: ASR: removing serviceId from ArtifactStream
// TODO: ASR: add feature flag: should not have serviceId in ArtifactStream
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "ArtifactStreamKeys")
@Entity(value = "artifactStream")
@HarnessEntity(exportable = true)
public abstract class ArtifactStream
    extends Base implements AccountAccess, ArtifactSourceable, PersistentRegularIterable, NameAccess, KeywordsAware {
  protected static final String dateFormat = "HHMMSS";

  @Transient private String artifactStreamId;
  private String artifactStreamType;
  private String sourceName;
  private String settingId;
  @Transient private String artifactServerName;
  @EntityName private String name;
  private boolean autoPopulate;
  @Indexed private String serviceId;
  @Transient private Service service;
  @Deprecated private transient boolean autoDownload;
  @Deprecated private transient boolean autoApproveForProduction;
  private boolean metadataOnly;
  private int failedCronAttempts;
  @Indexed private Long nextIteration;
  @Indexed private Long nextCleanupIteration;
  private String templateUuid;
  private String templateVersion;
  private List<Variable> templateVariables = new ArrayList<>();
  private String accountId;
  @SchemaIgnore private Set<String> keywords;
  @Transient private long artifactCount;
  @Transient private List<ArtifactSummary> artifacts;
  private boolean sample;
  String[] perpetualTaskIds; // reference to the perpertual tasks

  // Collection status denotes whether the first-time collection of the artifact stream is completed. If it's completed
  // we mark the status as STABLE, otherwise it is by default UNSTABLE when the stream is created or the artifact source
  // is updated. For backwards compatibility, null is treated as stable and we make sure to update the artifact stream
  // to UNSTABLE if the artifact source is changed.
  private String collectionStatus;

  public ArtifactStream(String artifactStreamType) {
    this.artifactStreamType = artifactStreamType;
    this.autoPopulate = true;
  }

  public ArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, String entityYamlPath, String artifactStreamType, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, boolean metadataOnly, String accountId, Set<String> keywords,
      boolean sample) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.artifactStreamType = artifactStreamType;
    this.sourceName = sourceName;
    this.settingId = settingId;
    this.name = name;
    this.autoPopulate = autoPopulate;
    this.serviceId = serviceId;
    this.metadataOnly = metadataOnly;
    this.accountId = accountId;
    this.keywords = keywords;
    this.sample = sample;
  }

  public String fetchAppId() {
    return appId;
  }

  public String generateName() {
    return Utils.normalize(generateSourceName());
  }

  public abstract String generateSourceName();

  public abstract ArtifactStreamAttributes fetchArtifactStreamAttributes();

  public abstract String fetchArtifactDisplayName(String buildNo);

  public void validateRequiredFields() {}

  public boolean artifactSourceChanged(ArtifactStream artifactStream) {
    return !this.sourceName.equals(artifactStream.getSourceName());
  }

  public boolean shouldValidate() {
    return false;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return ArtifactStreamKeys.nextCleanupIteration.equals(fieldName) ? nextCleanupIteration : nextIteration;
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    if (ArtifactStreamKeys.nextCleanupIteration.equals(fieldName)) {
      this.nextCleanupIteration = nextIteration;
      return;
    }

    this.nextIteration = nextIteration;
  }

  @Override
  public Set<String> generateKeywords() {
    Set<String> keywords = KeywordsAware.super.generateKeywords();
    keywords.addAll(asList(name, sourceName, artifactStreamType));
    return keywords;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public abstract static class Yaml extends BaseEntityYaml {
    private String serverName;
    private String templateUri;
    private List<NameValuePair> templateVariables;

    public Yaml(String type, String harnessApiVersion, String serverName) {
      super(type, harnessApiVersion);
      this.serverName = serverName;
    }

    public Yaml(String type, String harnessApiVersion) {
      super(type, harnessApiVersion);
    }

    public Yaml(String type, String harnessApiVersion, String serverName, boolean metadataOnly, String templateUri,
        List<NameValuePair> templateVariables) {
      super(type, harnessApiVersion);
      this.serverName = serverName;
      this.templateUri = templateUri;
      this.templateVariables = templateVariables;
    }
  }

  @UtilityClass
  public static final class ArtifactStreamKeys {
    // Temporary
    public static final String appId = "appId";
    public static final String uuid = "uuid";
    public static final String settingId = "settingId";
    public static final String repositoryFormat = "repositoryFormat";
    public static final String repositoryType = "repositoryType";
  }
}
