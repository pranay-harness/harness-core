package software.wings.security.encryption;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;
import software.wings.security.EncryptionType;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created by rsingh on 9/29/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(value = "encryptedRecords", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EncryptedData extends Base {
  @NotEmpty private String encryptionKey;
  @NotEmpty private char[] encryptedValue;
  @NotEmpty private SettingVariableTypes type;

  @NotEmpty @Indexed private String parentId;

  @NotEmpty @Indexed private String accountId;

  private boolean enabled = true;

  @NotEmpty private String kmsId;

  @NotEmpty private EncryptionType encryptionType;

  private Map<Long, EmbeddedUser> updates = new HashMap<>();

  public void addToUpdatedBy(long updatedAt, EmbeddedUser updatedBy) {
    updates.put(updatedAt, updatedBy);
  }

  @JsonIgnore
  public Set<Pair<Long, EmbeddedUser>> getAllUpdates() {
    if (updates.size() <= 1) {
      return Collections.emptySet();
    }

    SortedMap<Long, EmbeddedUser> sortedUpdates = new TreeMap<>();
    sortedUpdates.putAll(updates);
    sortedUpdates.remove(sortedUpdates.firstKey());
    Set<Pair<Long, EmbeddedUser>> rv = new TreeSet<>(Collections.reverseOrder());
    for (Entry<Long, EmbeddedUser> entry : sortedUpdates.entrySet()) {
      rv.add(new ImmutablePair<>(entry.getKey(), entry.getValue()));
    }
    return rv;
  }

  @Override
  public String toString() {
    return "EncryptedData{"
        + "type=" + type + ", parentId='" + parentId + '\'' + ", accountId='" + accountId + '\''
        + ", enabled=" + enabled + ", kmsId='" + kmsId + '\'' + ", appId='" + appId + '\'' + "} " + super.toString();
  }
}
