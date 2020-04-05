package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.validator.EntityNameValidator.ALLOWED_CHARS_SERVICE_VARIABLE_MESSAGE;
import static io.harness.data.validator.EntityNameValidator.ALLOWED_CHARS_SERVICE_VARIABLE_STRING;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.data.validator.EntityName;
import io.harness.encryption.Encrypted;
import io.harness.encryption.EncryptionReflectUtils;
import io.harness.security.encryption.EncryptionType;
import io.harness.validation.Create;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
import org.mongodb.morphia.utils.IndexType;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.artifact.ArtifactStreamSummary;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 9/14/16.
 */
@Indexes({
  @Index(fields =
      {
        @Field("entityId")
        , @Field("templateId"), @Field("overrideType"), @Field("instances"), @Field("expression"), @Field("type"),
            @Field("name")
      },
      options = @IndexOptions(unique = true, name = "serviceVariableUniqueIdx"))
  ,
      @Index(fields = { @Field("appId")
                        , @Field("entityId") }, options = @IndexOptions(name = "app_entityId")),
      @Index(fields = { @Field("appId")
                        , @Field("envId"), @Field("templateId") },
          options = @IndexOptions(name = "app_env_templateId")),
      @Index(options = @IndexOptions(name = "appEntityIdx"), fields = {
        @Field("appId"), @Field("entityId"), @Field(value = "createdAt", type = IndexType.DESC),
      })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"encryptedValue", "encryptedBy"})
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "ServiceVariableKeys")
@Entity(value = "serviceVariables", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class ServiceVariable extends Base implements EncryptableSetting {
  /**
   * The constant DEFAULT_TEMPLATE_ID.
   */
  public static final String DEFAULT_TEMPLATE_ID = "__TEMPLATE_ID";
  public static final String ENCRYPTED_VALUE_KEY = "encryptedValue";

  private String templateId = DEFAULT_TEMPLATE_ID;

  @NotEmpty(groups = {Create.class}) private String envId;

  @NotNull(groups = {Create.class}) private EntityType entityType;

  @NotEmpty(groups = {Create.class}) private String entityId;

  private String parentServiceVariableId;

  @Transient private ServiceVariable overriddenServiceVariable;

  private OverrideType overrideType;

  private List<String> instances;
  private String expression;

  @SchemaIgnore private String accountId;

  @NotEmpty(groups = {Create.class})
  @EntityName(charSetString = ALLOWED_CHARS_SERVICE_VARIABLE_STRING, message = ALLOWED_CHARS_SERVICE_VARIABLE_MESSAGE)
  private String name;

  @Encrypted(fieldName = "value") private char[] value;

  private Type type;

  // NOTE: This field is used for service variables of type artifact.
  private List<String> allowedList;

  @Indexed @SchemaIgnore private String encryptedValue;

  @SchemaIgnore @Transient private String secretTextName;

  @JsonIgnore @SchemaIgnore @Transient private boolean decrypted;

  @SchemaIgnore @Transient private String serviceId;

  @SchemaIgnore @Transient private transient EncryptionType encryptionType;

  @SchemaIgnore @Transient private transient String encryptedBy;

  private transient List<ArtifactStreamSummary> artifactStreamSummaries;

  @Override
  @SchemaIgnore
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.SERVICE_VARIABLE;
  }

  public void setSettingType(SettingVariableTypes type) {
    //
  }

  @Override
  @JsonIgnore
  @SchemaIgnore
  public List<java.lang.reflect.Field> getEncryptedFields() {
    if (type != Type.ENCRYPTED_TEXT) {
      return Collections.emptyList();
    }

    return EncryptionReflectUtils.getEncryptedFields(this.getClass());
  }

  /**
   * The enum Type.
   */
  public enum Type {
    /**
     * Text type.
     */
    TEXT,
    /**
     * Lb type.
     */
    LB(true),
    /**
     * Encrypted text type.
     */
    ENCRYPTED_TEXT,
    /**
     * Artifact type.
     */
    ARTIFACT;

    private boolean settingAttribute;

    Type() {}

    Type(boolean settingAttribute) {
      this.settingAttribute = settingAttribute;
    }

    /**
     * Is setting attribute boolean.
     *
     * @return the boolean
     */
    public boolean isSettingAttribute() {
      return settingAttribute;
    }
  }

  public enum OverrideType {
    /**
     * All  override type.
     */
    ALL,
    /**
     * Instances override type.
     */
    INSTANCES,
    /**
     * Custom override type.
     */
    CUSTOM
  }

  public ServiceVariable cloneInternal() {
    ServiceVariable serviceVariable = builder()
                                          .accountId(getAccountId())
                                          .envId(getEnvId())
                                          .entityId(getEntityId())
                                          .entityType(getEntityType())
                                          .templateId(getTemplateId())
                                          .name(getName())
                                          .value(getValue())
                                          .type(getType())
                                          .encryptedValue(getEncryptedValue())
                                          .expression(getExpression())
                                          .overrideType(getOverrideType())
                                          .build();

    serviceVariable.setAppId(getAppId());
    if (isNotEmpty(getAllowedList())) {
      serviceVariable.setAllowedList(new ArrayList<>(getAllowedList()));
    }
    return serviceVariable;
  }

  @UtilityClass
  public static final class ServiceVariableKeys {
    // Temporary
    public static final String appId = "appId";
    public static final String createdAt = "createdAt";
    public static final String uuid = "uuid";
  }
}
