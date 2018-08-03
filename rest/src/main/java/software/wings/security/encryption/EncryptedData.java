package software.wings.security.encryption;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
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
import software.wings.settings.UsageRestrictions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
  public static final String NAME_KEY = "name";

  @NotEmpty @Indexed private String name;

  @NotEmpty private String encryptionKey;
  @NotEmpty private char[] encryptedValue;
  @NotEmpty private SettingVariableTypes type;

  @NotEmpty @Indexed @Default private Set<String> parentIds = new HashSet<>();

  @NotEmpty @Indexed private String accountId;

  @Builder.Default private boolean enabled = true;

  @NotEmpty private String kmsId;

  @NotEmpty private EncryptionType encryptionType;

  @NotEmpty private long fileSize;

  @Default private List<String> appIds = new ArrayList<>();

  @Default private List<String> serviceIds = new ArrayList<>();

  @Default private List<String> envIds = new ArrayList<>();

  private Set<String> serviceVariableIds;

  private Map<String, AtomicInteger> searchTags;

  private UsageRestrictions usageRestrictions;

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

  public void addApplication(String appId, String appName) {
    if (appIds == null) {
      appIds = new ArrayList<>();
    }
    appIds.add(appId);
    addSearchTag(appName);
  }

  public void removeApplication(String appId, String appName) {
    removeReference(appId, appName, appIds);
  }

  public void addService(String serviceId, String serviceName) {
    if (serviceIds == null) {
      serviceIds = new ArrayList<>();
    }
    serviceIds.add(serviceId);
    addSearchTag(serviceName);
  }

  public void removeService(String serviceId, String serviceName) {
    removeReference(serviceId, serviceName, serviceIds);
  }

  public void addEnvironment(String envId, String environmentName) {
    if (envIds == null) {
      envIds = new ArrayList<>();
    }
    envIds.add(envId);
    addSearchTag(environmentName);
  }

  public void removeEnvironment(String envId, String envName) {
    removeReference(envId, envName, envIds);
  }

  public void addServiceVariable(String serviceVariableId, String serviceVariableName) {
    if (serviceVariableIds == null) {
      serviceVariableIds = new HashSet<>();
    }
    serviceVariableIds.add(serviceVariableId);
    addSearchTag(serviceVariableName);
  }

  public void removeServiceVariable(String serviceVariableId, String serviceVariableName) {
    if (!isEmpty(serviceVariableIds)) {
      serviceVariableIds.remove(serviceVariableId);
    }

    if (!isEmpty(searchTags)) {
      searchTags.remove(serviceVariableName);
    }
  }

  private void addSearchTag(String searchTag) {
    if (searchTags == null) {
      searchTags = new HashMap<>();
    }

    if (searchTags.containsKey(searchTag)) {
      searchTags.get(searchTag).incrementAndGet();
    } else {
      searchTags.put(searchTag, new AtomicInteger(1));
    }

    if (getKeywords() == null) {
      setKeywords(new ArrayList<>());
    }
    if (!getKeywords().contains(searchTag)) {
      getKeywords().add(searchTag);
    }
  }

  private void removeReference(String key, String searchTag, List<String> collection) {
    collection.remove(key);
    int refCount = searchTags.get(searchTag).decrementAndGet();
    if (refCount == 0) {
      searchTags.remove(searchTag);
      if (getKeywords() != null) {
        getKeywords().remove(searchTag);
      }
    }
  }

  public void clearSearchTags() {
    if (!isEmpty(appIds)) {
      appIds.clear();
    }

    if (!isEmpty(serviceIds)) {
      serviceIds.clear();
    }

    if (!isEmpty(envIds)) {
      envIds.clear();
    }

    if (!isEmpty(serviceVariableIds)) {
      serviceVariableIds.clear();
    }

    if (!isEmpty(searchTags)) {
      searchTags.clear();
    }
  }

  @Override
  public String toString() {
    return "EncryptedData{"
        + "name='" + name + '\'' + ", type=" + type + ", parentIds=" + parentIds + ", accountId='" + accountId + '\''
        + ", enabled=" + enabled + ", kmsId='" + kmsId + '\'' + ", encryptionType=" + encryptionType + ", encryptedBy='"
        + encryptedBy + '\'' + "} " + super.toString();
  }
}
