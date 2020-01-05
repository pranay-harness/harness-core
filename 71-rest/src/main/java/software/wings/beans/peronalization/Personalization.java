package software.wings.beans.peronalization;

import io.harness.annotation.HarnessEntity;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.peronalization.Personalization.Steps.StepsKeys;
import software.wings.beans.peronalization.Personalization.Templates.TemplatesKeys;

import java.util.LinkedList;
import java.util.Set;

@Value
@Builder
@Indexes(@Index(options = @IndexOptions(name = "identification", unique = true),
    fields = { @Field("accountId")
               , @Field("userId") }))
@FieldNameConstants(innerTypeName = "PersonalizationKeys")
@Entity(value = "personalization", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class Personalization implements PersistentEntity {
  @Id private ObjectId id;

  private String accountId;
  private String userId;

  @Value
  @Builder
  @FieldNameConstants(innerTypeName = "StepsKeys")
  public static class Steps {
    private Set<String> favorites;
    private LinkedList<String> recent;
  }

  private Steps steps;

  @Value
  @Builder
  @FieldNameConstants(innerTypeName = "TemplatesKeys")
  public static class Templates {
    private Set<String> favorites;
  }

  private Templates templates;

  @UtilityClass
  public static final class PersonalizationKeys {
    public static final String steps_favorites = steps + "." + StepsKeys.favorites;
    public static final String steps_recent = steps + "." + StepsKeys.recent;
    public static final String templates_favorites = templates + "." + TemplatesKeys.favorites;
  }
}
