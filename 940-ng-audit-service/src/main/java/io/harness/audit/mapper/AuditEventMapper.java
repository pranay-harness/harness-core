package io.harness.audit.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.AuditCommonConstants;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.entities.AuditEvent;
import io.harness.ng.core.common.beans.KeyValuePair;
import io.harness.ng.core.mapper.KeyValuePairMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class AuditEventMapper {
  public static AuditEvent fromDTO(AuditEventDTO dto) {
    List<KeyValuePair> additionalInfo = KeyValuePairMapper.convertToList(dto.getAdditionalInfo());
    if (isEmpty(additionalInfo)) {
      additionalInfo = null;
    }
    List<KeyValuePair> coreInfo = new ArrayList<>();
    if (isNotEmpty(dto.getResourceScope().getOrgIdentifier())) {
      coreInfo.add(KeyValuePair.builder()
                       .key(AuditCommonConstants.ORG_IDENTIFIER)
                       .value(dto.getResourceScope().getOrgIdentifier())
                       .build());
    }
    if (isNotEmpty(dto.getResourceScope().getProjectIdentifier())) {
      coreInfo.add(KeyValuePair.builder()
                       .key(AuditCommonConstants.PROJECT_IDENTIFIER)
                       .value(dto.getResourceScope().getProjectIdentifier())
                       .build());
    }
    if (isNotEmpty(dto.getResourceScope().getLabels())) {
      dto.getResourceScope().getLabels().forEach(
          (key, value) -> coreInfo.add(KeyValuePair.builder().key(key).value(value).build()));
    }
    coreInfo.add(KeyValuePair.builder().key(AuditCommonConstants.MODULE).value(dto.getModule().name()).build());
    coreInfo.add(KeyValuePair.builder().key(AuditCommonConstants.ACTION).value(dto.getAction().name()).build());
    coreInfo.add(KeyValuePair.builder()
                     .key(AuditCommonConstants.ENVIRONMENT_IDENTIFIER)
                     .value(dto.getEnvironmentIdentifier())
                     .build());
    return AuditEvent.builder()
        .insertId(dto.getInsertId())
        .resourceScope(ResourceScopeMapper.fromDTO(dto.getResourceScope()))
        .httpRequestInfo(dto.getHttpRequestInfo())
        .requestMetadata(dto.getRequestMetadata())
        .timestamp(dto.getTimestamp())
        .authenticationInfo(AuthenticationInfoMapper.fromDTO(dto.getAuthenticationInfo()))
        .resource(ResourceMapper.fromDTO(dto.getResource()))
        .yamlDiff(dto.getYamlDiff())
        .auditEventData(dto.getAuditEventData())
        .module(dto.getModule())
        .action(dto.getAction())
        .environmentIdentifier(dto.getEnvironmentIdentifier())
        .coreInfo(coreInfo)
        .additionalInfo(additionalInfo)
        .build();
  }

  public static AuditEventDTO toDTO(AuditEvent auditEvent) {
    Map<String, String> additionalInfo = KeyValuePairMapper.convertToMap(auditEvent.getAdditionalInfo());
    if (isEmpty(additionalInfo)) {
      additionalInfo = null;
    }
    return AuditEventDTO.builder()
        .insertId(auditEvent.getInsertId())
        .resourceScope(ResourceScopeMapper.toDTO(auditEvent.getResourceScope()))
        .httpRequestInfo(auditEvent.getHttpRequestInfo())
        .requestMetadata(auditEvent.getRequestMetadata())
        .timestamp(auditEvent.getTimestamp())
        .authenticationInfo(AuthenticationInfoMapper.toDTO(auditEvent.getAuthenticationInfo()))
        .module(auditEvent.getModule())
        .resource(ResourceMapper.toDTO(auditEvent.getResource()))
        .action(auditEvent.getAction())
        .environmentIdentifier(auditEvent.getEnvironmentIdentifier())
        .yamlDiff(auditEvent.getYamlDiff())
        .auditEventData(auditEvent.getAuditEventData())
        .additionalInfo(additionalInfo)
        .build();
  }
}
