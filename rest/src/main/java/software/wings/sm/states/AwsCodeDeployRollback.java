package software.wings.sm.states;

import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.api.AwsCodeDeployRequestElement;
import software.wings.api.CommandStateExecutionData.Builder;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CodeDeployParams;
import software.wings.common.Constants;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;

import java.util.List;

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
      List<EncryptedDataDetail> encryptedDataDetails, Builder executionDataBuilder) {
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

  @Override
  @SchemaIgnore
  public boolean isIgnoreApplicationStopFailures() {
    return super.isIgnoreApplicationStopFailures();
  }

  @Override
  @SchemaIgnore
  public String getFileExistsBehavior() {
    return super.getFileExistsBehavior();
  }

  @Override
  @SchemaIgnore
  public boolean isEnableAutoRollback() {
    return super.isEnableAutoRollback();
  }

  @Override
  @SchemaIgnore
  public List<String> getAutoRollbackConfigurations() {
    return super.getAutoRollbackConfigurations();
  }
}
