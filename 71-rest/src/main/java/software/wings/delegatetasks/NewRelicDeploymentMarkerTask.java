package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.NewRelicDeploymentMarkerPayload;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.sm.StateType;

import java.util.function.Consumer;
import java.util.function.Supplier;
public class NewRelicDeploymentMarkerTask extends AbstractDelegateRunnableTask {
  @Inject private NewRelicDelegateService newRelicDelegateService;

  public NewRelicDeploymentMarkerTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public ResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public ResponseData run(TaskParameters parameters) {
    NewRelicDataCollectionInfo dataCollectionInfo = (NewRelicDataCollectionInfo) parameters;
    if (isEmpty(dataCollectionInfo.getDeploymentMarker())) {
      throw new WingsException("Empty deployment marker body");
    }
    try {
      NewRelicDeploymentMarkerPayload payload =
          JsonUtils.asObject(dataCollectionInfo.getDeploymentMarker(), NewRelicDeploymentMarkerPayload.class);
      newRelicDelegateService.postDeploymentMarker(dataCollectionInfo.getNewRelicConfig(),
          dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(), payload,
          ThirdPartyApiCallLog.builder()
              .accountId(getAccountId())
              .delegateId(getDelegateId())
              .delegateTaskId(getTaskId())
              .stateExecutionId(dataCollectionInfo.getStateExecutionId())
              .build());
      return DataCollectionTaskResult.builder()
          .status(DataCollectionTaskResult.DataCollectionTaskStatus.SUCCESS)
          .newRelicDeploymentMarkerBody(dataCollectionInfo.getDeploymentMarker())
          .build();
    } catch (Exception ex) {
      return DataCollectionTaskResult.builder()
          .status(DataCollectionTaskResult.DataCollectionTaskStatus.FAILURE)
          .stateType(StateType.NEW_RELIC)
          .errorMessage("Could not send deployment marker : " + ExceptionUtils.getMessage(ex))
          .newRelicDeploymentMarkerBody(dataCollectionInfo.getDeploymentMarker())
          .build();
    }
  }
}
