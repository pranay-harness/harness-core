package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;

import java.util.Date;

@Entity(value = "delegateConnections", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class DelegateConnection extends Base {
  public static final int defaultExpiryTimeInMinutes = 5;

  @NotEmpty private String accountId;
  @NotEmpty private String delegateId;
  private String version;
  private long lastHeartbeat;

  @SchemaIgnore @JsonIgnore @Indexed(options = @IndexOptions(expireAfterSeconds = 0)) private Date validUntil;
}
