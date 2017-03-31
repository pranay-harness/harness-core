/**
 *
 */

package software.wings.api;

import software.wings.common.Constants;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;

import java.util.List;
import java.util.Map;

/**
 * The type Service instance ids param.
 *
 * @author Rishi
 */
public class InstanceElementListParam implements ContextElement {
  private List<InstanceElement> instanceElements;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PARAM;
  }

  @Override
  public String getUuid() {
    return null;
  }

  @Override
  public String getName() {
    return Constants.INSTANCE_LIST_PARAMS;
  }

  @Override
  public Map<String, Object> paramMap() {
    return null;
  }

  public List<InstanceElement> getInstanceElements() {
    return instanceElements;
  }

  public void setInstanceElements(List<InstanceElement> instanceElements) {
    this.instanceElements = instanceElements;
  }

  public static final class InstanceElementListParamBuilder {
    private List<InstanceElement> instanceElements;

    private InstanceElementListParamBuilder() {}

    public static InstanceElementListParamBuilder anInstanceElementListParam() {
      return new InstanceElementListParamBuilder();
    }

    public InstanceElementListParamBuilder withInstanceElements(List<InstanceElement> instanceElements) {
      this.instanceElements = instanceElements;
      return this;
    }

    public InstanceElementListParam build() {
      InstanceElementListParam instanceElementListParam = new InstanceElementListParam();
      instanceElementListParam.setInstanceElements(instanceElements);
      return instanceElementListParam;
    }
  }
}
