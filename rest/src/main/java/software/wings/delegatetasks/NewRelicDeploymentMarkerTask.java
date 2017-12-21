package software.wings.delegatetasks;

import software.wings.beans.DelegateTask;
import software.wings.beans.NewRelicDeploymentMarkerPayload;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.intfc.newrelic.NewRelicDelegateService;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.NotifyResponseData;
import wiremock.org.apache.commons.lang.StringUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;

public class NewRelicDeploymentMarkerTask extends AbstractDelegateRunnableTask {
  @Inject private NewRelicDelegateService newRelicDelegateService;

  public NewRelicDeploymentMarkerTask(String delegateId, DelegateTask delegateTask,
      Consumer<NotifyResponseData> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public NotifyResponseData run(Object[] parameters) {
    NewRelicDataCollectionInfo dataCollectionInfo = (NewRelicDataCollectionInfo) parameters[0];
    if (StringUtils.isEmpty(dataCollectionInfo.getDeploymentMarker())) {
      throw new WingsException("Empty deployment marker body");
    }
    try {
      NewRelicDeploymentMarkerPayload payload =
          JsonUtils.asObject(dataCollectionInfo.getDeploymentMarker(), NewRelicDeploymentMarkerPayload.class);
      newRelicDelegateService.postDeploymentMarker(dataCollectionInfo.getNewRelicConfig(),
          dataCollectionInfo.getEncryptedDataDetails(), dataCollectionInfo.getNewRelicAppId(), payload);
      return DataCollectionTaskResult.builder()
          .status(DataCollectionTaskResult.DataCollectionTaskStatus.SUCCESS)
          .build();
    } catch (Exception ex) {
      return DataCollectionTaskResult.builder()
          .status(DataCollectionTaskResult.DataCollectionTaskStatus.FAILURE)
          .stateType(StateType.NEW_RELIC)
          .errorMessage("Could not send deployment marker : " + ex.getMessage())
          .build();
    }
  }
}
