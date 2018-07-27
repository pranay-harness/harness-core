package software.wings.beans;

import static io.harness.data.validator.EntityNameValidator.ALLOWED_CHARS_SERVICE_VARIABLE_MESSAGE;
import static io.harness.data.validator.EntityNameValidator.ALLOWED_CHARS_SERVICE_VARIABLE_STRING;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.data.validator.EntityName;
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
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.security.EncryptionType;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.WingsReflectionUtils;
import software.wings.utils.validation.Create;

import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 9/14/16.
 */
@Entity(value = "serviceVariables", noClassnameStored = true)
@Indexes(@Index(fields =
    {
      @Field("entityId")
      , @Field("templateId"), @Field("overrideType"), @Field("instances"), @Field("expression"), @Field("type"),
          @Field("name")
    },
    options = @IndexOptions(unique = true, name = "serviceVariableUniqueIdx")))
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceVariable extends Base implements Encryptable {
  /**
   * The constant DEFAULT_TEMPLATE_ID.
   */
  public static final String DEFAULT_TEMPLATE_ID = "__TEMPLATE_ID";

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

  @Encrypted private char[] value;

  private Type type;

  @SchemaIgnore private String encryptedValue;

  @SchemaIgnore @Transient private String secretTextName;

  @SchemaIgnore @Transient private boolean decrypted;

  @SchemaIgnore @Transient private String serviceId;

  @SchemaIgnore @Transient private transient EncryptionType encryptionType;

  @SchemaIgnore @Transient private transient String encryptedBy;

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

    return WingsReflectionUtils.getEncryptedFields(this.getClass());
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
    ENCRYPTED_TEXT;

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
    return serviceVariable;
  }
}
