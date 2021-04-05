package harness.callgraph.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class StackNode {
  private final String packageName;
  private final String className;
  private final String methodName;
  private final String signature;
  private final boolean testMethod;

  public StackNode(String type, String methodName, String signature, boolean testMethod) {
    this.packageName = getPackageName(type);
    this.className = getClassName(type);
    this.methodName = methodName;
    this.signature = signature;
    this.testMethod = testMethod;
  }

  public String getPackageName(String type) {
    int indexDot = type.lastIndexOf('.');
    return type.substring(0, indexDot);
  }

  public String getClassName(String type) {
    int indexDot = type.lastIndexOf('.');
    return type.substring(indexDot + 1);
  }
}
