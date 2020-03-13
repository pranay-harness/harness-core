package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;

import java.util.Date;
import javax.validation.constraints.NotNull;

@FieldNameConstants(innerTypeName = "DelegateConnectionKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@Entity(value = "delegateConnections", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class DelegateConnection implements PersistentEntity, UuidAware {
  public static final int DEFAULT_EXPIRY_TIME_IN_MINUTES = 5;

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;

  @NotEmpty private String accountId;
  @NotEmpty private String delegateId;
  private String version;
  private long lastHeartbeat;

  @JsonIgnore @SchemaIgnore @Indexed(options = @IndexOptions(expireAfterSeconds = 0)) private Date validUntil;
}
