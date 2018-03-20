package software.wings.beans;

import lombok.Data;
import org.mongodb.morphia.annotations.Entity;

import java.util.List;

@Entity(value = "idempotent")
@Data
public class Idempotent extends Base {
  public static final String TENTATIVE = "tentative";
  public static final String SUCCEEDED = "succeeded";

  private String state;
  private List<Object> result;
}
