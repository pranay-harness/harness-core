package software.wings.sm.states;

import static software.wings.utils.Misc.getMessage;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;

public class AwsAmiRollbackSwitchRoutesState extends AwsAmiSwitchRoutesState {
  public AwsAmiRollbackSwitchRoutesState(String name) {
    super(name, StateType.AWS_AMI_ROLLBACK_SWITCH_ROUTES.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context, true);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  @Override
  @SchemaIgnore
  public boolean isDownsizeOldAsg() {
    return super.isDownsizeOldAsg();
  }

  @Override
  @SchemaIgnore
  public void setDownsizeOldAsg(boolean downsizeOldAsg) {
    super.setDownsizeOldAsg(downsizeOldAsg);
  }
}