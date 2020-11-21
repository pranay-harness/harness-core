package software.wings.beans.peronalization;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;

import software.wings.beans.peronalization.PersonalizationSteps.PersonalizationStepsKeys;
import software.wings.beans.peronalization.PersonalizationTemplates.PersonalizationTemplatesKeys;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Value
@Builder
@NgUniqueIndex(name = "identification", fields = { @Field("accountId")
                                                   , @Field("userId") })
@FieldNameConstants(innerTypeName = "PersonalizationKeys")
@Entity(value = "personalization", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class Personalization implements PersistentEntity, AccountAccess {
  @Id private ObjectId id;

  private String accountId;
  private String userId;

  private PersonalizationSteps steps;

  private PersonalizationTemplates templates;

  @UtilityClass
  public static final class PersonalizationKeys {
    public static final String steps_favorites = steps + "." + PersonalizationStepsKeys.favorites;
    public static final String steps_recent = steps + "." + PersonalizationStepsKeys.recent;
    public static final String templates_favorites = templates + "." + PersonalizationTemplatesKeys.favorites;
  }
}
