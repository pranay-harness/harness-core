package software.wings.sm.states;

import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.api.AwsCodeDeployRequestElement;
import software.wings.api.CommandStateExecutionData.Builder;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CodeDeployParams;
import software.wings.common.Constants;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;

/**
 * Created by rishi on 6/26/17.
 */
public class AwsCodeDeployRollback extends AwsCodeDeployState {
  public AwsCodeDeployRollback(String name) {
    super(name, StateType.AWS_CODEDEPLOY_ROLLBACK.name());
  }

  @Override
  protected CodeDeployParams prepareCodeDeployParams(ExecutionContext context,
      CodeDeployInfrastructureMapping infrastructureMapping, SettingAttribute cloudProviderSetting,
      Builder executionDataBuilder) {
    AwsCodeDeployRequestElement codeDeployRequestElement =
        context.getContextElement(ContextElementType.PARAM, Constants.AWS_CODE_DEPLOY_REQUEST_PARAM);
    executionDataBuilder.withCodeDeployParams(codeDeployRequestElement.getOldCodeDeployParams());
    return codeDeployRequestElement.getOldCodeDeployParams();
  }

  @Override
  @SchemaIgnore
  public String getBucket() {
    return super.getBucket();
  }

  @Override
  @SchemaIgnore
  public String getKey() {
    return super.getKey();
  }

  @Override
  @SchemaIgnore
  public String getBundleType() {
    return super.getBundleType();
  }
}
