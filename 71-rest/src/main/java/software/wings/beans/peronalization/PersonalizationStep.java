package software.wings.beans.peronalization;

import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.Set;

@Value
@Builder
@Entity(value = "personalizationSteps", noClassnameStored = true)
public class PersonalizationStep implements PersistentEntity {
  public static final String ACCOUNT_USER_ID_KEY = "accountUserId";
  public static final String FAVORITES_KEY = "favorites";

  @Id private String accountUserId;
  private Set<String> favorites;
}
