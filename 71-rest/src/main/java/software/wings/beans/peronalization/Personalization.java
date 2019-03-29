package software.wings.beans.peronalization;

import io.harness.annotation.HarnessExportableEntity;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Value;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.LinkedList;
import java.util.Set;

@Value
@Builder
@HarnessExportableEntity
@Entity(value = "personalization", noClassnameStored = true)
@Indexes(@Index(options = @IndexOptions(name = "identification", unique = true),
    fields = { @Field("accountId")
               , @Field("userId") }))
public class Personalization implements PersistentEntity {
  public static final String ACCOUNT_ID_KEY = "accountId";
  public static final String USER_ID_KEY = "userId";
  public static final String STEPS_KEY = "steps";
  public static final String STEPS_FAVORITES_KEY = STEPS_KEY + ".favorites";
  public static final String STEPS_RECENT_KEY = STEPS_KEY + ".recent";

  @Id private ObjectId id;

  private String accountId;
  private String userId;

  @Value
  @Builder
  public static class Steps {
    private Set<String> favorites;
    private LinkedList<String> recent;
  }

  private Steps steps;
}
