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

import java.util.Set;
import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "CommandEntityKeys")
@Entity(value = "clCommands", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class CommandEntity implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id @NotNull(groups = {Update.class}) String uuid;
  String commandStoreName;
  String type;
  String name;
  String description;
  String repoUrl;
  Set<String> tags;
  String imageUrl;
  String latestVersion;
  long createdAt;
  String lastUpdatedByAccountId;
  long lastUpdatedAt;
  String createdByAccountId;
}
