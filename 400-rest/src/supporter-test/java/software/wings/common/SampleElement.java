package software.wings.common;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;

import io.harness.annotations.dev.TargetModule;
import io.harness.context.ContextElementType;

import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.Map;

/**
 * The type Sample element.
 */
@TargetModule(_957_CG_BEANS)
public class SampleElement implements ContextElement {
  private String uuid;

  /**
   * Instantiates a new Sample element.
   *
   * @param uuid the uuid
   */
  public SampleElement(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public ContextElementType getElementType() {
    return null;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }
}
