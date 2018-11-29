package software.wings.settings.validation;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXISTING_PROPERTY;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.hibernate.validator.constraints.NotEmpty;
import ro.fortsoft.pf4j.ExtensionPoint;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXISTING_PROPERTY)
public abstract class ConnectivityValidationAttributes implements ExtensionPoint {
  @NotEmpty private String type;
}