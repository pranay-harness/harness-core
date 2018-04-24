/**
 *
 */

package software.wings.api;

import software.wings.common.Constants;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Map;

/**
 * The type Service instance ids param.
 *
 * @author Rishi
 */
public class InstanceElementListParam implements ContextElement {
  private List<InstanceElement> instanceElements;
  private List<PcfInstanceElement> pcfInstanceElements;

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
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  public List<InstanceElement> getInstanceElements() {
    return instanceElements;
  }

  public void setInstanceElements(List<InstanceElement> instanceElements) {
    this.instanceElements = instanceElements;
  }

  public List<PcfInstanceElement> getPcfInstanceElements() {
    return pcfInstanceElements;
  }

  public void setPcfInstanceElements(List<PcfInstanceElement> pcfInstanceElements) {
    this.pcfInstanceElements = pcfInstanceElements;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  public static final class InstanceElementListParamBuilder {
    private List<InstanceElement> instanceElements;
    private List<PcfInstanceElement> pcfInstanceElements;

    private InstanceElementListParamBuilder() {}

    public static InstanceElementListParamBuilder anInstanceElementListParam() {
      return new InstanceElementListParamBuilder();
    }

    public InstanceElementListParamBuilder withInstanceElements(List<InstanceElement> instanceElements) {
      this.instanceElements = instanceElements;
      return this;
    }

    public InstanceElementListParamBuilder withPcfInstanceElements(List<PcfInstanceElement> pcfInstanceElements) {
      this.pcfInstanceElements = pcfInstanceElements;
      return this;
    }

    public InstanceElementListParam build() {
      InstanceElementListParam instanceElementListParam = new InstanceElementListParam();
      instanceElementListParam.setInstanceElements(instanceElements);
      instanceElementListParam.setPcfInstanceElements(pcfInstanceElements);
      return instanceElementListParam;
    }
  }
}
