package software.wings.beans.commandlibrary;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import software.wings.beans.Variable;
import software.wings.beans.template.BaseTemplate;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "CommandVersionsKeys")
@Entity(value = "clCommandVersions", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class CommandVersionEntity
    implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware {
  @Id @NotNull(groups = {Update.class}) String uuid;
  String commandName;
  String commandStoreName;
  String version;
  String description;
  String yamlContent;
  BaseTemplate templateObject;
  List<Variable> variables;
  EmbeddedUser createdBy;
  long createdAt;
  EmbeddedUser lastUpdatedBy;
  long lastUpdatedAt;
}
