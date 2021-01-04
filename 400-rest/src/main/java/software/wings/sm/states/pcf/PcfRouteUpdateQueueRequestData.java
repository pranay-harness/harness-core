package software.wings.sm.states.pcf;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.Application;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PcfRouteUpdateQueueRequestData {
  private Application app;
  private PcfConfig pcfConfig;
  private PcfInfrastructureMapping pcfInfrastructureMapping;
  private String activityId;
  private String envId;
  private EnvironmentType environmentType;
  private Integer timeoutIntervalInMinutes;
  private String commandName;
  private PcfRouteUpdateRequestConfigData requestConfigData;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private boolean skipRollback;
  private boolean downsizeOldApps;
  private boolean useCfCli;
}
