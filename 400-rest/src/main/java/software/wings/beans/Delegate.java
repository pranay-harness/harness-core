package software.wings.beans;

import static java.time.Duration.ofDays;

import io.harness.annotation.HarnessEntity;
import io.harness.delegate.beans.DelegateSizeDetails;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@FieldNameConstants(innerTypeName = "DelegateKeys")
@Entity(value = "delegates", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class Delegate implements PersistentEntity, UuidAware, CreatedAtAware, AccountAccess, PersistentRegularIterable {
  public static final Duration TTL = ofDays(30);

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @SchemaIgnore @FdIndex private long createdAt;
  // Will be used by ECS delegate, when hostName is mentioned in TaskSpec.
  @NotEmpty @FdIndex private String accountId;

  // Will be used for NG to uniquely identify the delegate during the installation process, together with the accountId.
  // It will be populated by the backend and will be available as a property in the delegate installation files.
  @FdIndex private String sessionIdentifier;

  // Will be used for NG to hold delegate size details
  private DelegateSizeDetails sizeDetails;

  @Default private Status status = Status.ENABLED;
  private String description;
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
  private boolean polllingModeEnabled;
  private boolean proxy;
  private boolean ceEnabled;

  @Deprecated private List<String> supportedTaskTypes;

  @Transient private List<String> currentlyExecutingDelegateTasks;

  @Transient private boolean useCdn;

  @Transient private String useJreVersion;

  @Transient private String location;

  private List<DelegateScope> includeScopes;
  private List<DelegateScope> excludeScopes;
  private List<String> tags;
  private String profileResult;
  private boolean profileError;
  private long profileExecutedAt;
  private boolean sampleDelegate;

  @FdIndex Long capabilitiesCheckNextIteration;

  @FdTtlIndex private Date validUntil;

  @SchemaIgnore private List<String> keywords;

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (DelegateKeys.capabilitiesCheckNextIteration.equals(fieldName)) {
      this.capabilitiesCheckNextIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (DelegateKeys.capabilitiesCheckNextIteration.equals(fieldName)) {
      return this.capabilitiesCheckNextIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  public enum Status { ENABLED, WAITING_FOR_APPROVAL, @Deprecated DISABLED, DELETED }
}
