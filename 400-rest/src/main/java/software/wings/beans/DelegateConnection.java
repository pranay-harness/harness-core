package software.wings.beans;

import static java.time.Duration.ofDays;
import static java.time.Duration.ofMinutes;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@FieldNameConstants(innerTypeName = "DelegateConnectionKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@Entity(value = "delegateConnections", noClassnameStored = true)
@HarnessEntity(exportable = false)
@TargetModule(Module._420_DELEGATE_SERVICE)
public class DelegateConnection implements PersistentEntity, UuidAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("index2")
                 .field(DelegateConnectionKeys.accountId)
                 .field(DelegateConnectionKeys.delegateId)
                 .field(DelegateConnectionKeys.version)
                 .build())
        .build();
  }

  public static final Duration TTL = ofDays(15);
  public static final Duration EXPIRY_TIME = ofMinutes(5);

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  @NotEmpty private String accountId;
  @NotEmpty private String delegateId;
  private String version;
  private String location;
  private long lastHeartbeat;
  private boolean disconnected;

  @JsonIgnore @SchemaIgnore @FdTtlIndex private Date validUntil;
}
