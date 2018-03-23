package software.wings.beans;

import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;

@Entity(value = "idempotent_locks", noClassnameStored = true)
@Data
public class Idempotent extends Base {
  public static final String TENTATIVE = "tentative";
  public static final String SUCCEEDED = "succeeded";

  private String state;
  private List<Object> result;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(3).toInstant());
}
