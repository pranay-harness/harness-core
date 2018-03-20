package software.wings.beans;

import lombok.Data;
import org.mongodb.morphia.annotations.Entity;

@Entity(value = "idempotent", noClassnameStored = true)
@Data
public class Idempotent extends Base {
  public static final String TENTATIVE = "tentative";
  public static final String SUCCEEDED = "succeeded";

  private String state;
}
