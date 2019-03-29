package software.wings.beans.peronalization;

import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Value;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.LinkedList;
import java.util.Set;

@Value
@Builder
@Entity(value = "personalization", noClassnameStored = true)
public class Personalization implements PersistentEntity {
  public static final String ACCOUNT_ID_KEY = "accountId";
  public static final String USER_ID_KEY = "userId";
  public static final String FAVORITES_KEY = "favorites";
  public static final String RECENT_KEY = "recent";

  @Id private ObjectId id;

  private String accountId;
  private String userId;
  private Set<String> favorites;
  private LinkedList<String> recent;
}
