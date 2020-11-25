package io.harness.states;

import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.status.BuildStatusPushResponse;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.task.ci.CIBuildStatusPushParameters;
import io.harness.facilitator.modes.task.TaskExecutable;
import io.harness.git.GitClientHelper;
import io.harness.ng.core.NGAccess;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.ngpipeline.orchestration.StepUtils;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.Status;
import io.harness.pms.steps.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;
import io.harness.stateutils.buildstate.ConnectorUtils;
import io.harness.tasks.ResponseData;
import io.harness.tasks.Task;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuildStatusStep implements TaskExecutable<BuildStatusUpdateParameter> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType("COMMIT_STATUS").build();
  private static final int socketTimeoutMillis = 10000;
  @Inject GitClientHelper gitClientHelper;
  @Inject private ConnectorUtils connectorUtils;

  @Override
  public Class<BuildStatusUpdateParameter> getStepParametersClass() {
    return BuildStatusUpdateParameter.class;
  }

  @Override
  public Task obtainTask(Ambiance ambiance, BuildStatusUpdateParameter stepParameters, StepInputPackage inputPackage) {
    NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);
    ConnectorDetails gitConnector = getGitConnector(ngAccess, stepParameters.getConnectorIdentifier());
    GitConfigDTO gitConfigDTO = (GitConfigDTO) gitConnector.getConnectorConfig();

    CIBuildStatusPushParameters ciBuildPushStatusParameters =
        CIBuildStatusPushParameters.builder()
            .desc(stepParameters.getDesc())
            .sha(stepParameters.getSha())
            .owner(gitClientHelper.getGitOwner(gitConfigDTO.getUrl()))
            .repo(gitClientHelper.getGitRepo(gitConfigDTO.getUrl()))
            .identifier(stepParameters.getIdentifier())
            .state(stepParameters.getState())
            .build();

    // TODO Take this info from github app connector
    ciBuildPushStatusParameters.setKey("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDM5xLSJEkinAIL\n"
        + "qxjE965bAaZLoBuLJiKdkBnrckdSfGgqdilHSkaVEgRSnG7JQcumahbt/7nOV7Yk\n"
        + "BPFPOtfphikGZVgM9yNPn+PH7jEtXTnnkSClwq+onWcOFSlc5/17EWDTk5xVMdqx\n"
        + "m6WfgF+JdCah1+GoVAmmm4G0yub/00ZCKooItQzz0BQm9qEHpHSre5sCt7wNgtH3\n"
        + "RaD+dQKRJMAfcpWdAJcjIKPX6HlhfOMTb49vDLDTMgScZT17eOgr8QvedGgye5bz\n"
        + "MOWhsRnjS4cgGbcTq1ftGYE4neIX7TbyX0SEt8gg7MXe+tJjMh/+7+bTRUKQYJpC\n"
        + "sPQFTrQBAgMBAAECggEAYU9MLOh2oy0b+5aiCMjn0OiTpU7ARfEyd0m8RYjcPlw+\n"
        + "zAuZxvWLV7havTD1nDbXFI1FnnnYMBqPscN3Jn13lLvWN+dhTacA6guxDX4ddMHV\n"
        + "ghf2PUKcUaOPEa0TG8BBLXUvWsu7bupiRf75RSqeNJUo06vGyz495xXrH4VM9yjx\n"
        + "nTOkddwl1LK07KJ95zeMUn4o+NkaWMUboEUGlC2hPdFAk/EWatgzXOVNvcWHeceK\n"
        + "tWBAPyIy4Q97sNeFiuvcs5LL7dZ19ITUMcVbAS9CxrxhjVmqpFJo5AEY+SZe+WbN\n"
        + "36Dm/WSHV1LXZVzJkEkRiZBrDtp+hMp57CFBg+z5wQKBgQDpTIvIFm1LL6Bk7/+T\n"
        + "Uwr91Lpcv861w65KoX2ekrXla27mSfTK4AOph0E5DTNC6UdGfeGDhmuJ0d/+Bb/z\n"
        + "6Yz+dnVcQXGsYBBCjhbZt6dLYDpSSffFU4hp707IkfneM3a4uCTH2kKyUpLKi4Q2\n"
        + "Vv6ELGV//u0HMRQ3EtIREuKRHQKBgQDg1y08lnMW8G7n+Uf34NbDDYwDrGfUj2L8\n"
        + "OEOYAqCSA9XHGolBPzhcinZ5q6fYR9qBd20qWMz1oJsf88LQA8/iebTv7cuHMHWW\n"
        + "u/Jcqhf8uJITbZSrQs8nlKACGYCUhoy9aNvX2PucJAHAsgSu9OtPmlSJxqUs3nOE\n"
        + "VFTSY2H9NQKBgQCOjWw4BaQgtehO5OsIrUxhD1QUiksXe4sLJSQp+cFVftDTvErs\n"
        + "j/cM5o1u++bfssUPiKl8gW1CWFCC2iaRNpslfWJ2zbJUvpoQ4NuLixGZGCJq17Gj\n"
        + "DEilWkmMes3v/QhFFJe82lu4tIXnZ1qRDZUVVD9s92sD4vRUNpbPQffY7QKBgQDR\n"
        + "4sxNtLw2+7bsQV4XXQHeDzVW8If0eu2SOQuQSVOPOplDRdg+2j9I09CI/96tHVYy\n"
        + "aUO0tjSOTqDAkRKYkBZteeOX3cmSp3/9d/Fk4zuFJN7n1/FidflfH3TGwPuwqnGT\n"
        + "FuGyetFWDp68PPH2SJepNY4ZFyB15Cq9quOLik6cyQKBgChmTS4xeMeQsCyLdU0e\n"
        + "BnXIseynAoLTUwnrAPs53NXSLlbS9zCLJvDCDegHpLz5g4fD4pyB+/i5h3Pbm7Q5\n"
        + "pkfEfyhF0NRY9eOy059rRQplTvpK+u+vnA9wLL6iIMgfQTcVWkaz8GJH9H1Y5eQq\n"
        + "BBqMchCoiaQTrwy010MSgqyV");
    ciBuildPushStatusParameters.setInstallId("12472752");
    ciBuildPushStatusParameters.setAppId("85357");

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .parked(false)
                                  .timeout(TaskData.DEFAULT_ASYNC_CALL_TIMEOUT)
                                  .taskType("BUILD_STATUS")
                                  .parameters(new Object[] {ciBuildPushStatusParameters})
                                  .build();
    return StepUtils.prepareDelegateTaskInput(
        ambiance.getSetupAbstractions().get("accountId"), taskData, ambiance.getSetupAbstractions());
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, BuildStatusUpdateParameter stepParameters, Map<String, ResponseData> responseDataMap) {
    BuildStatusPushResponse executionResponse = (BuildStatusPushResponse) responseDataMap.values().iterator().next();

    if (executionResponse.getStatus() == BuildStatusPushResponse.Status.SUCCESS) {
      return StepResponse.builder().status(Status.SUCCEEDED).build();
    } else {
      return StepResponse.builder().status(Status.FAILED).build();
    }
  }

  private ConnectorDetails getGitConnector(NGAccess ngAccess, String connectorRef) {
    return connectorUtils.getConnectorDetails(ngAccess, connectorRef);
  }
}
