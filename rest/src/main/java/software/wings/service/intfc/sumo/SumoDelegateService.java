package software.wings.service.intfc.sumo;

import software.wings.beans.SumoConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;

import java.io.IOException;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by sriram_parthasarathy on 9/11/17.
 */
public interface SumoDelegateService {
  @DelegateTaskType(TaskType.SUMO_VALIDATE_CONFIGURATION_TASK)
  boolean validateConfig(@NotNull SumoConfig sumoConfig, List<EncryptedDataDetail> encryptedDataDetails)
      throws IOException;
}
