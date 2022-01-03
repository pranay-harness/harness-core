package io.harness.ng.core.service.services;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.Long.parseLong;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.InstanceDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.service.instance.InstanceService;

import com.google.inject.Inject;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.PIPELINE)
public class ServiceEntityManagementServiceImpl implements ServiceEntityManagementService {
  private final InstanceService instanceService;
  private final ServiceEntityService serviceEntityService;

  @Override
  public boolean deleteService(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, String ifMatch) {
    List<InstanceDTO> instanceInfoNGList = instanceService.getActiveInstancesByServiceId(
        accountId, orgIdentifier, projectIdentifier, serviceIdentifier, System.currentTimeMillis());
    if (isNotEmpty(instanceInfoNGList)) {
      throw new InvalidRequestException(String.format(
          "Service [%s] under Project[%s], Organization [%s] couldn't be deleted since there are currently %d active instances for the service",
          serviceIdentifier, projectIdentifier, orgIdentifier, instanceInfoNGList.size()));
    }
    return serviceEntityService.delete(
        accountId, orgIdentifier, projectIdentifier, serviceIdentifier, isNumeric(ifMatch) ? parseLong(ifMatch) : null);
  }
}
