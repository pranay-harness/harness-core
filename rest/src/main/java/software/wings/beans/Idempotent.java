package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

@Entity(value = "idempotent_locks", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = false)
public class Idempotent extends Base {
  public static final String STATE_KEY = "state";
  public static final String RESULT_KEY = "result";
  public static final String VALID_UNTIL_KEY = "validUntil";

  public static final String TENTATIVE = "tentative";
  public static final String SUCCEEDED = "succeeded";

  private String state;
  private List<Object> result;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(3).toInstant());
}
