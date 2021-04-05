package io.harness.audit.mapper;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.entities.AuditEvent;
import io.harness.ng.core.common.beans.KeyValuePair;
import io.harness.ng.core.mapper.KeyValuePairMapper;

import java.util.List;
import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class AuditEventMapper {
  public static AuditEvent fromDTO(AuditEventDTO dto) {
    List<KeyValuePair> internalInfo = KeyValuePairMapper.convertToList(dto.getInternalInfo());
    if (isEmpty(internalInfo)) {
      internalInfo = null;
    }
    return AuditEvent.builder()
        .insertId(dto.getInsertId())
        .resourceScope(ResourceScopeMapper.fromDTO(dto.getResourceScope()))
        .httpRequestInfo(dto.getHttpRequestInfo())
        .requestMetadata(dto.getRequestMetadata())
        .timestamp(dto.getTimestamp())
        .authenticationInfo(AuthenticationInfoMapper.fromDTO(dto.getAuthenticationInfo()))
        .module(dto.getModule())
        .resource(ResourceMapper.fromDTO(dto.getResource()))
        .action(dto.getAction())
        .auditEventData(dto.getAuditEventData())
        .internalInfo(internalInfo)
        .build();
  }

  public static AuditEventDTO toDTO(AuditEvent auditEvent) {
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
        .auditEventData(auditEvent.getAuditEventData())
        .build();
  }
}
