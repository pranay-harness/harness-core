package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;

import java.util.List;
import javax.validation.constraints.NotNull;

@Entity(value = "delegates", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@Indexes({ @Index(fields = { @Field("accountId") }, options = @IndexOptions(name = "delegateAccountIdIdx")) })
@HarnessExportableEntity
@FieldNameConstants(innerTypeName = "DelegateKeys")
public class Delegate implements PersistentEntity, UuidAware, CreatedAtAware, PersistentRegularIterable {
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @SchemaIgnore @Indexed private long createdAt;
  // Will be used by ECS delegate, when hostName is mentioned in TaskSpec.
  @NotEmpty private String accountId;
  @Default private Status status = Status.ENABLED;
  private String description;
  private boolean connected;
  private String ip;
  private String hostName;
  private String delegateGroupName;
  private String delegateName;
  private String delegateProfileId;
  private long lastHeartBeat;
  private String version;
  private transient String sequenceNum;
  private String delegateType;
  private transient String delegateRandomToken;
  private transient boolean keepAlivePacket;
  private transient boolean polllingModeEnabled;
  @Indexed Long nextRecentlyDisconnectedIteration;

  @Deprecated private List<String> supportedTaskTypes;

  @Transient private List<String> currentlyExecutingDelegateTasks;

  private List<DelegateScope> includeScopes;
  private List<DelegateScope> excludeScopes;
  private List<String> tags;
  private String profileResult;
  private boolean profileError;
  private long profileExecutedAt;
  private boolean sampleDelegate;

  @SchemaIgnore private List<String> keywords;

  public enum Status { ENABLED, DISABLED, DELETED }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextRecentlyDisconnectedIteration;
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    this.nextRecentlyDisconnectedIteration = nextIteration;
  }
}
