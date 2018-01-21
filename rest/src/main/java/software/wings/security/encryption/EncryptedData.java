package software.wings.security.encryption;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.security.EncryptionType;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rsingh on 9/29/17.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(value = "encryptedRecords", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Indexes({
  @Index(fields = { @Field("name"), @Field("accountId") }, options = @IndexOptions(unique = true, name = "uniqueIdx"))
})
public class EncryptedData extends Base {
  @NotEmpty @Indexed private String name;

  @NotEmpty private String encryptionKey;
  @NotEmpty private char[] encryptedValue;
  @NotEmpty private SettingVariableTypes type;

  @NotEmpty @Indexed @Builder.Default private Set<String> parentIds = new HashSet<>();

  @NotEmpty @Indexed private String accountId;

  @Builder.Default private boolean enabled = true;

  @NotEmpty private String kmsId;

  @NotEmpty private EncryptionType encryptionType;

  @NotEmpty private long fileSize;

  @SchemaIgnore @Transient private transient String encryptedBy;

  @SchemaIgnore @Transient private transient int setupUsage;

  @SchemaIgnore @Transient private transient long runTimeUsage;

  @SchemaIgnore @Transient private transient int changeLog;

  public void addParent(String parentId) {
    if (parentIds == null) {
      parentIds = new HashSet<>();
    }

    parentIds.add(parentId);
  }

  public void removeParentId(String parentId) {
    if (parentIds == null) {
      return;
    }

    parentIds.remove(parentId);
  }

  @Override
  public String toString() {
    return "EncryptedData{"
        + "name='" + name + '\'' + ", type=" + type + ", parentIds=" + parentIds + ", accountId='" + accountId + '\''
        + ", enabled=" + enabled + ", kmsId='" + kmsId + '\'' + ", encryptionType=" + encryptionType + ", encryptedBy='"
        + encryptedBy + '\'' + "} " + super.toString();
  }
}
