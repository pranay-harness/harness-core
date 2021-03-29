package io.harness.audit.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
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
        .coreInfo(coreInfo)
        .additionalInfo(additionalInfo)
        .build();
  }

  public static AuditEventDTO toDTO(AuditEvent auditEvent) {
    Map<String, String> additionalInfo = KeyValuePairMapper.convertToMap(auditEvent.getAdditionalInfo());
    if (isEmpty(additionalInfo)) {
      additionalInfo = null;
    }
    Map<String, String> coreInfo = KeyValuePairMapper.convertToMap(auditEvent.getCoreInfo());
    ModuleType module = ModuleType.valueOf(coreInfo.get(AuditCommonConstants.MODULE));
    Action action = Action.valueOf(coreInfo.get(AuditCommonConstants.ACTION));
    String environmentIdentifier = coreInfo.get(AuditCommonConstants.ENVIRONMENT_IDENTIFIER);
    return AuditEventDTO.builder()
        .insertId(auditEvent.getInsertId())
        .resourceScope(ResourceScopeMapper.toDTO(auditEvent.getResourceScope()))
        .httpRequestInfo(auditEvent.getHttpRequestInfo())
        .requestMetadata(auditEvent.getRequestMetadata())
        .timestamp(auditEvent.getTimestamp())
        .authenticationInfo(AuthenticationInfoMapper.toDTO(auditEvent.getAuthenticationInfo()))
        .module(module)
        .resource(ResourceMapper.toDTO(auditEvent.getResource()))
        .action(action)
        .environmentIdentifier(environmentIdentifier)
        .yamlDiff(auditEvent.getYamlDiff())
        .auditEventData(auditEvent.getAuditEventData())
        .additionalInfo(additionalInfo)
        .build();
  }
}
