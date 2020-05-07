package software.wings.beans.commandlibrary;

import io.harness.annotation.HarnessEntity;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
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
import java.util.Set;
import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "CommandVersionsKeys")
@Entity(value = "clCommandVersions", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class CommandVersionEntity implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id @NotNull(groups = {Update.class}) String uuid;
  String commandName;
  String commandStoreName;
  String commandId;
  String version;
  String description;
  String yamlContent;
  String repoUrl;
  Set<String> tags;
  BaseTemplate templateObject;
  List<Variable> variables;
  long createdAt;
  String lastUpdatedByAccountId;
  long lastUpdatedAt;
  String createdByAccountId;
}
