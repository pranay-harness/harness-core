/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;

import com.github.reinert.jjschema.SchemaIgnore;

@OwnedBy(CDP)
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
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
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

  @Override
  @SchemaIgnore
  public Integer getTimeoutMillis() {
    return super.getTimeoutMillis();
  }
}
