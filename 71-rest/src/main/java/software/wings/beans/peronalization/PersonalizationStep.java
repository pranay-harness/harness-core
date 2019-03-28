package software.wings.beans.peronalization;

import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.LinkedList;
import java.util.Set;

@Value
@Builder
@Entity(value = "personalizationSteps", noClassnameStored = true)
public class PersonalizationStep implements PersistentEntity {
  public static final String ACCOUNT_USER_ID_KEY = "accountUserId";
  public static final String FAVORITES_KEY = "favorites";
  public static final String RECENT_KEY = "recent";

  @Id private String accountUserId;
  private Set<String> favorites;
  private LinkedList<String> recent;
}
