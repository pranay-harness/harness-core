package io.harness.cvng.cd10.services.impl;

import io.harness.cvng.cd10.beans.CD10MappingsDTO;
import io.harness.cvng.cd10.beans.MappingType;
import io.harness.cvng.cd10.entities.CD10EnvMapping;
import io.harness.cvng.cd10.entities.CD10Mapping;
import io.harness.cvng.cd10.entities.CD10Mapping.CD10MappingKeys;
import io.harness.cvng.cd10.entities.CD10ServiceMapping;
import io.harness.cvng.cd10.services.api.CD10MappingService;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import org.mongodb.morphia.query.Query;

public class CD10MappingServiceImpl implements CD10MappingService {
  @Inject private HPersistence hPersistence;
  @Override
  public void create(String accountId, CD10MappingsDTO cd10MappingsDTO) {
    Query<CD10Mapping> deleteQuery =
        hPersistence.createQuery(CD10Mapping.class)
            .filter(CD10MappingKeys.accountId, cd10MappingsDTO.getAccountId())
            .filter(CD10MappingKeys.orgIdentifier, cd10MappingsDTO.getOrgIdentifier())
            .filter(CD10MappingKeys.projectIdentifier, cd10MappingsDTO.getProjectIdentifier());
    hPersistence.delete(deleteQuery);
    hPersistence.save(cd10MappingsDTO.getEnvMappings()
                          .stream()
                          .map(envMappingDTO
                              -> CD10EnvMapping.builder()
                                     .accountId(accountId)
                                     .appId(envMappingDTO.getAppId())
                                     .envId(envMappingDTO.getEnvId())
                                     .type(envMappingDTO.getType())
                                     .projectIdentifier(cd10MappingsDTO.getProjectIdentifier())
                                     .orgIdentifier(cd10MappingsDTO.getOrgIdentifier())
                                     .envIdentifier(envMappingDTO.getEnvIdentifier())
                                     .build())
                          .collect(Collectors.toList()));
    hPersistence.save(cd10MappingsDTO.getServiceMappings()
                          .stream()
                          .map(envMappingDTO
                              -> CD10ServiceMapping.builder()
                                     .accountId(accountId)
                                     .appId(envMappingDTO.getAppId())
                                     .serviceId(envMappingDTO.getServiceId())
                                     .type(envMappingDTO.getType())
                                     .projectIdentifier(cd10MappingsDTO.getProjectIdentifier())
                                     .orgIdentifier(cd10MappingsDTO.getOrgIdentifier())
                                     .serviceIdentifier(envMappingDTO.getServiceIdentifier())
                                     .build())
                          .collect(Collectors.toList()));
  }

  @Override
  public CD10MappingsDTO list(String accountId, String orgIdentifier, String projectIdentifier) {
    List<CD10Mapping> cd10Mappings = hPersistence.createQuery(CD10Mapping.class)
                                         .filter(CD10MappingKeys.accountId, accountId)
                                         .filter(CD10MappingKeys.orgIdentifier, orgIdentifier)
                                         .filter(CD10MappingKeys.projectIdentifier, projectIdentifier)
                                         .asList();

    return CD10MappingsDTO.builder()
        .accountId(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .envMappings(cd10Mappings.stream()
                         .filter(cd10Mapping -> cd10Mapping.getType() == MappingType.ENV_MAPPING)
                         .map(cd10Mapping -> (CD10EnvMapping) cd10Mapping)
                         .map(cd10EnvMapping -> cd10EnvMapping.toDTO())
                         .collect(Collectors.toSet()))
        .serviceMappings(cd10Mappings.stream()
                             .filter(cd10Mapping -> cd10Mapping.getType() == MappingType.SERVICE_MAPPING)
                             .map(cd10Mapping -> (CD10ServiceMapping) cd10Mapping)
                             .map(cd10ServiceMapping -> cd10ServiceMapping.toDTO())
                             .collect(Collectors.toSet()))
        .build();
  }
}
