package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.stencils.DataProvider;
import software.wings.stencils.EnumData;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 1/10/17.
 */
@JsonTypeName("AWS")
public class AwsInfrastructureMapping extends InfrastructureMapping {
  @Attributes(title = "Restrictions")
  @EnumData(enumDataProvider = AwsInfrastructureRestrictionProvider.class)
  private String restrictionType;
  @Attributes(title = "Expression") private String restrictionExpression;

  /**
   * Instantiates a new Aws infrastructure mapping.
   */
  public AwsInfrastructureMapping() {
    super(SettingVariableTypes.AWS.name());
  }

  /**
   * Gets restriction type.
   *
   * @return the restriction type
   */
  public String getRestrictionType() {
    return restrictionType;
  }

  /**
   * Sets restriction type.
   *
   * @param restrictionType the restriction type
   */
  public void setRestrictionType(String restrictionType) {
    this.restrictionType = restrictionType;
  }

  /**
   * Gets restriction expression.
   *
   * @return the restriction expression
   */
  public String getRestrictionExpression() {
    return restrictionExpression;
  }

  /**
   * Sets restriction expression.
   *
   * @param restrictionExpression the restriction expression
   */
  public void setRestrictionExpression(String restrictionExpression) {
    this.restrictionExpression = restrictionExpression;
  }

  public static class AwsInfrastructureRestrictionProvider implements DataProvider {
    @Override
    public Map<String, String> getData(String appId, String... params) {
      return Arrays.stream(RestrictionType.values())
          .collect(Collectors.toMap(RestrictionType::name, RestrictionType::getDisplayName));
    }
  }

  /**
   * The enum Restriction type.
   */
  public enum RestrictionType {
    /**
     * None restriction type.
     */
    NONE("None"), /**
                   * Instance restriction type.
                   */
    INSTANCE("By specific instances"), /**
                                        * Custom restriction type.
                                        */
    CUSTOM("By zone/tags etc");

    RestrictionType(String displayName) {
      this.displayName = displayName;
    }

    private String displayName;

    public String getDisplayName() {
      return displayName;
    }

    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }
  }
}
