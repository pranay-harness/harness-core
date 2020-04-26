package software.wings.security.encryption;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.settings.SettingValue.SettingVariableTypes.SECRET_TEXT;
import static software.wings.settings.SettingValue.SettingVariableTypes.SERVICE_VARIABLE;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.NameAccess;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.FeatureName;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.usage.scope.ScopedEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 9/29/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"encryptionKey", "encryptedValue", "backupEncryptionKey", "backupEncryptedValue"})
@EqualsAndHashCode(callSuper = false)
@Entity(value = "encryptedRecords", noClassnameStored = true)
@HarnessEntity(exportable = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Indexes({
  @Index(fields = { @Field("accountId")
                    , @Field("name") }, options = @IndexOptions(unique = true, name = "acctNameIdx"))
  , @Index(fields = { @Field("accountId")
                      , @Field("kmsId") }, options = @IndexOptions(name = "acctKmsIdx"))
})
@FieldNameConstants(innerTypeName = "EncryptedDataKeys")
public class EncryptedData
    extends Base implements EncryptedRecord, NameAccess, PersistentRegularIterable, AccountAccess, ScopedEntity {
  @Inject @SchemaIgnore @Transient private static FeatureFlagService featureFlagService;

  @NotEmpty @Indexed private String name;

  @NotEmpty @Indexed private String encryptionKey;

  @NotEmpty private char[] encryptedValue;

  // When 'path' value is set, no actual encryption is needed since it's just referring to a secret in a Vault path.
  @Indexed private String path;

  @NotEmpty private SettingVariableTypes type;

  @NotEmpty @Default @Indexed private Set<String> parentIds = new HashSet<>();

  @NotEmpty @Default @Indexed private Set<EncryptedDataParent> parents = new HashSet<>();

  @NotEmpty private String accountId;

  @Default private boolean enabled = true;

  @NotEmpty private String kmsId;

  @NotEmpty private EncryptionType encryptionType;

  @NotEmpty private long fileSize;

  @Default private List<String> appIds = new ArrayList<>();

  @Default private List<String> serviceIds = new ArrayList<>();

  @Default private List<String> envIds = new ArrayList<>();

  private char[] backupEncryptedValue;

  private String backupEncryptionKey;

  private String backupKmsId;

  private EncryptionType backupEncryptionType;

  private Set<String> serviceVariableIds;

  private Map<String, AtomicInteger> searchTags;

  private boolean scopedToAccount;

  private UsageRestrictions usageRestrictions;

  @Indexed private Long nextMigrationIteration;

  @Indexed private Long nextAwsToGcpKmsMigrationIteration;

  @SchemaIgnore private boolean base64Encoded;

  @SchemaIgnore @Transient private transient String encryptedBy;

  @SchemaIgnore @Transient private transient int setupUsage;

  @SchemaIgnore @Transient private transient long runTimeUsage;

  @SchemaIgnore @Transient private transient int changeLog;

  @SchemaIgnore @Indexed private List<String> keywords;

  @Deprecated
  public Set<String> getParentIds() {
    return parentIds;
  }

  public boolean areParentIdsEquivalentToParent() {
    Set<String> derivedParentIds = parents.stream().map(EncryptedDataParent::getId).collect(Collectors.toSet());
    return derivedParentIds.equals(parentIds);
  }

  public void addParent(@NotNull EncryptedDataParent encryptedDataParent) {
    if (!featureFlagService.isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, accountId)) {
      parentIds.add(encryptedDataParent.getId());
    }
    parents.add(encryptedDataParent);
  }

  public void removeParent(@NotNull EncryptedDataParent encryptedDataParent) {
    if (!featureFlagService.isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, accountId)) {
      parentIds.remove(encryptedDataParent.getId());
    }
    parents.remove(encryptedDataParent);
  }

  public boolean containsParent(@NotNull String id, @NotNull SettingVariableTypes type) {
    if (featureFlagService.isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, accountId)) {
      return parents.stream().anyMatch(
          encryptedDataParent -> encryptedDataParent.getId().equals(id) && encryptedDataParent.getType() == type);
    }
    return parentIds.contains(id);
  }

  public Set<EncryptedDataParent> getParents() {
    if (featureFlagService.isEnabled(FeatureName.SECRET_PARENTS_MIGRATED, accountId)) {
      return parents;
    }
    return parentIds.stream()
        .map(id -> {
          SettingVariableTypes settingType = type == SECRET_TEXT ? SERVICE_VARIABLE : type;
          return EncryptedDataParent.builder().id(id).type(settingType).build();
        })
        .collect(Collectors.toSet());
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    if (EncryptedDataKeys.nextMigrationIteration.equals(fieldName)) {
      this.nextMigrationIteration = nextIteration;
      return;
    } else if (EncryptedDataKeys.nextAwsToGcpKmsMigrationIteration.equals(fieldName)) {
      this.nextAwsToGcpKmsMigrationIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (EncryptedDataKeys.nextMigrationIteration.equals(fieldName)) {
      return nextMigrationIteration;
    } else if (EncryptedDataKeys.nextAwsToGcpKmsMigrationIteration.equals(fieldName)) {
      return nextAwsToGcpKmsMigrationIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  public void addApplication(String appId, String appName) {
    if (appIds == null) {
      appIds = new ArrayList<>();
    }
    appIds.add(appId);
    addSearchTag(appName);
  }

  public void removeApplication(String appId, String appName) {
    removeSearchTag(appId, appName, appIds);
  }

  public void addService(String serviceId, String serviceName) {
    if (serviceIds == null) {
      serviceIds = new ArrayList<>();
    }
    serviceIds.add(serviceId);
    addSearchTag(serviceName);
  }

  public void removeService(String serviceId, String serviceName) {
    removeSearchTag(serviceId, serviceName, serviceIds);
  }

  public void addEnvironment(String envId, String environmentName) {
    if (envIds == null) {
      envIds = new ArrayList<>();
    }
    envIds.add(envId);
    addSearchTag(environmentName);
  }

  public void removeEnvironment(String envId, String envName) {
    removeSearchTag(envId, envName, envIds);
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

  public void addSearchTag(String searchTag) {
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

  public void removeSearchTag(String key, String searchTag, List<String> collection) {
    if (isNotEmpty(collection)) {
      collection.remove(key);
    }

    if (isNotEmpty(searchTags) && searchTags.containsKey(searchTag)
        && searchTags.get(searchTag).decrementAndGet() == 0) {
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

  @UtilityClass
  public static final class EncryptedDataKeys {
    // Temporary
    public static final String createdAt = "createdAt";
    public static final String uuid = "uuid";
    public static final String name = "name";
    public static final String accountId = "accountId";
  }
}
