package io.harness.cdng.pipeline.helpers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.dtos.InstanceDTO;
import io.harness.service.instance.InstanceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Row3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.row;

@Slf4j
@Singleton
public class CDPipelineInstrumentationHelper {
    @Inject InstanceService instanceService;

    public long getTotalNumberOfServiceInstanceInInterval(String accountIdentifier, long startInterval, long endInterval) {
        return getServiceInstancesInInterval(accountIdentifier, startInterval, endInterval).size();
    }

    public long getTotalNumberOfActiveServices(String accountIdentifier, long startInterval, long endInterval) {
        return getTotalNumberOfActiveServices(getServiceInstancesInInterval(accountIdentifier, startInterval, endInterval));
    }

    public long getTotalNumberOfActiveServices(List<InstanceDTO> serviceInstances) {
        Row3<String, String, String>[] activeServicesRows = getActiveServices(serviceInstances);
        return activeServicesRows.length;
    }

    public List<InstanceDTO> getServiceInstancesInInterval(String accountIdentifier, long startInterval, long endInterval) {
        List<InstanceDTO> totalServiceInstances = new ArrayList<>();
        try {
            totalServiceInstances.addAll(instanceService.getInstancesDeployedInInterval(accountIdentifier, startInterval, endInterval));
        } catch (Exception e) {
            log.error("Problem with retrieving service instances list.", e);
        }

        return totalServiceInstances;
    }

    public Row3<String, String, String>[] getActiveServices(List<InstanceDTO> serviceInstances) {
        Row3<String, String, String>[] serviceRowsFromInstances = getOrgProjectServiceRows(serviceInstances);

        return serviceRowsFromInstances;
    }

    Row3<String, String, String>[] getOrgProjectServiceRows(List<InstanceDTO> instanceDTOList) {
        try {
            Map<String, UniqueServiceEntityId> uniqueServiceEntityIdMap =
                    instanceDTOList.stream().collect(Collectors.toMap(
                            this::getUniqueServiceOrgProjectId,
                            instanceDTO1
                                    -> new UniqueServiceEntityId(instanceDTO1.getServiceIdentifier(), instanceDTO1.getProjectIdentifier(),
                                    instanceDTO1.getOrgIdentifier()),
                            (entry1, entry2) -> entry1));

            Row3<String, String, String>[] orgProjectServiceRows = new Row3[uniqueServiceEntityIdMap.size()];

            int index = 0;
            for (UniqueServiceEntityId uniqueServiceEntityId : uniqueServiceEntityIdMap.values()) {
                orgProjectServiceRows[index++] = row(uniqueServiceEntityId.getOrgIdentifier(),
                        uniqueServiceEntityId.getProjectIdentifier(), uniqueServiceEntityId.getServiceIdentifier());
            }
            return orgProjectServiceRows;
        } catch(Exception e) {
            log.error("Problem with handling service instances list.", e);
        }
        return new Row3[0];
    }

    private String getUniqueServiceOrgProjectId(InstanceDTO instanceDTO) {
        return String.join(
                "&", instanceDTO.getOrgIdentifier(), instanceDTO.getProjectIdentifier(), instanceDTO.getServiceIdentifier());
    }

    private class UniqueServiceEntityId {
        @Getter private final String serviceIdentifier;
        @Getter private final String projectIdentifier;
        @Getter private final String orgIdentifier;

        private UniqueServiceEntityId(String serviceIdentifier, String projectIdentifier, String orgIdentifier) {
            this.serviceIdentifier = serviceIdentifier;
            this.projectIdentifier = projectIdentifier;
            this.orgIdentifier = orgIdentifier;
        }
    }
}
