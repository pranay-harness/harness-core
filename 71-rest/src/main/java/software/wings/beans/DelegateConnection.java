package software.wings.beans;

import static java.time.Duration.ofDays;
import static java.time.Duration.ofMinutes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.Index;
import io.harness.mongo.index.IndexOptions;
import io.harness.mongo.index.Indexed;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import software.wings.beans.DelegateConnection.DelegateConnectionKeys;

import java.time.Duration;
import java.util.Date;
import javax.validation.constraints.NotNull;

@FieldNameConstants(innerTypeName = "DelegateConnectionKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@Entity(value = "delegateConnections", noClassnameStored = true)
@HarnessEntity(exportable = false)
@Index(name = "index2",
    fields =
    {
      @Field(DelegateConnectionKeys.accountId)
      , @Field(DelegateConnectionKeys.delegateId), @Field(DelegateConnectionKeys.version)
    })
public class DelegateConnection implements PersistentEntity, UuidAware, AccountAccess {
  public static final Duration TTL = ofDays(15);
  public static final Duration EXPIRY_TIME = ofMinutes(5);

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  @NotEmpty private String accountId;
  @NotEmpty private String delegateId;
  private String version;
  private long lastHeartbeat;
  private boolean disconnected;

  @JsonIgnore @SchemaIgnore @Indexed(options = @IndexOptions(expireAfterSeconds = 0)) private Date validUntil;
}
