package software.wings.delegatetasks.pcf.pcftaskhandler;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.PcfConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.PcfRequestConfig;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfInstanceSyncRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfInstanceSyncResponse;

import com.google.inject.Singleton;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.InstanceDetail;

@NoArgsConstructor
@Singleton
@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public class PcfApplicationDetailsCommandTaskHandler extends PcfCommandTaskHandler {
  @Override
  public PcfCommandExecutionResponse executeTaskInternal(PcfCommandRequest pcfCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback,
      boolean isInstanceSync) {
    if (!(pcfCommandRequest instanceof PcfInstanceSyncRequest)) {
      throw new InvalidArgumentsException(Pair.of("pcfCommandRequest", "Must be instance of PcfInstanceSyncRequest"));
    }
    PcfCommandExecutionResponse pcfCommandExecutionResponse = PcfCommandExecutionResponse.builder().build();
    PcfInstanceSyncResponse pcfInstanceSyncResponse =
        PcfInstanceSyncResponse.builder()
            .organization(pcfCommandRequest.getOrganization())
            .name(((PcfInstanceSyncRequest) pcfCommandRequest).getPcfApplicationName())
            .space(pcfCommandRequest.getSpace())
            .build();
    pcfCommandExecutionResponse.setPcfCommandResponse(pcfInstanceSyncResponse);
    try {
      PcfConfig pcfConfig = pcfCommandRequest.getPcfConfig();
      encryptionService.decrypt(pcfConfig, encryptedDataDetails, isInstanceSync);

      PcfInstanceSyncRequest pcfInstanceSyncRequest = (PcfInstanceSyncRequest) pcfCommandRequest;
      PcfRequestConfig pcfRequestConfig =
          PcfRequestConfig.builder()
              .timeOutIntervalInMins(5)
              .applicationName(pcfInstanceSyncRequest.getPcfApplicationName())
              .userName(String.valueOf(pcfConfig.getUsername()))
              .password(String.valueOf(pcfConfig.getPassword()))
              .endpointUrl(pcfConfig.getEndpointUrl())
              .orgName(pcfCommandRequest.getOrganization())
              .spaceName(pcfCommandRequest.getSpace())
              .limitPcfThreads(pcfCommandRequest.isLimitPcfThreads())
              .ignorePcfConnectionContextCache(pcfCommandRequest.isIgnorePcfConnectionContextCache())
              .build();

      ApplicationDetail applicationDetail = pcfDeploymentManager.getApplicationByName(pcfRequestConfig);

      pcfInstanceSyncResponse.setGuid(applicationDetail.getId());
      pcfInstanceSyncResponse.setName(applicationDetail.getName());
      pcfInstanceSyncResponse.setOrganization(pcfCommandRequest.getOrganization());
      pcfInstanceSyncResponse.setSpace(pcfCommandRequest.getSpace());
      if (CollectionUtils.isNotEmpty(applicationDetail.getInstanceDetails())) {
        pcfInstanceSyncResponse.setInstanceIndices(
            applicationDetail.getInstanceDetails().stream().map(InstanceDetail::getIndex).collect(toList()));
      }

      pcfInstanceSyncResponse.setCommandExecutionStatus(CommandExecutionStatus.SUCCESS);
      pcfInstanceSyncResponse.setOutput(StringUtils.EMPTY);
    } catch (Exception e) {
      log.warn(new StringBuilder(128)
                   .append("Failed while collecting PCF Application Details For Application: ")
                   .append(((PcfInstanceSyncRequest) pcfCommandRequest).getPcfApplicationName())
                   .append(", with Error: ")
                   .append(e)
                   .toString());
      pcfInstanceSyncResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
      pcfInstanceSyncResponse.setOutput(ExceptionUtils.getMessage(e));
    }

    pcfCommandExecutionResponse.setErrorMessage(pcfInstanceSyncResponse.getOutput());
    pcfCommandExecutionResponse.setCommandExecutionStatus(pcfInstanceSyncResponse.getCommandExecutionStatus());
    pcfCommandExecutionResponse.setPcfCommandResponse(pcfInstanceSyncResponse);

    return pcfCommandExecutionResponse;
  }
}
