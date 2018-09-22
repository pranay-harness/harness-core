package software.wings.api;

import software.wings.beans.command.CodeDeployParams;
import software.wings.common.Constants;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.Map;

/**
 * Created by rishi on 6/26/17.
 */
public class AwsCodeDeployRequestElement implements ContextElement {
  private CodeDeployParams codeDeployParams;
  private CodeDeployParams oldCodeDeployParams;

  public CodeDeployParams getCodeDeployParams() {
    return codeDeployParams;
  }

  public void setCodeDeployParams(CodeDeployParams codeDeployParams) {
    this.codeDeployParams = codeDeployParams;
  }

  public CodeDeployParams getOldCodeDeployParams() {
    return oldCodeDeployParams;
  }

  public void setOldCodeDeployParams(CodeDeployParams oldCodeDeployParams) {
    this.oldCodeDeployParams = oldCodeDeployParams;
  }

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
    return Constants.AWS_CODE_DEPLOY_REQUEST_PARAM;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  public static final class AwsCodeDeployRequestElementBuilder {
    private CodeDeployParams codeDeployParams;
    private CodeDeployParams oldCodeDeployParams;

    private AwsCodeDeployRequestElementBuilder() {}

    public static AwsCodeDeployRequestElementBuilder anAwsCodeDeployRequestElement() {
      return new AwsCodeDeployRequestElementBuilder();
    }

    public AwsCodeDeployRequestElementBuilder withCodeDeployParams(CodeDeployParams codeDeployParams) {
      this.codeDeployParams = codeDeployParams;
      return this;
    }

    public AwsCodeDeployRequestElementBuilder withOldCodeDeployParams(CodeDeployParams oldCodeDeployParams) {
      this.oldCodeDeployParams = oldCodeDeployParams;
      return this;
    }

    public AwsCodeDeployRequestElement build() {
      AwsCodeDeployRequestElement awsCodeDeployRequestElement = new AwsCodeDeployRequestElement();
      awsCodeDeployRequestElement.setCodeDeployParams(codeDeployParams);
      awsCodeDeployRequestElement.setOldCodeDeployParams(oldCodeDeployParams);
      return awsCodeDeployRequestElement;
    }
  }
}
