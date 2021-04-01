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
        .resourceScope(dto.getResourceScope())
        .httpRequestInfo(dto.getHttpRequestInfo())
        .requestMetadata(dto.getRequestMetadata())
        .timestamp(dto.getTimestamp())
        .authenticationInfo(dto.getAuthenticationInfo())
        .module(dto.getModule())
        .resource(dto.getResource())
        .action(dto.getAction())
        .yamlDiff(dto.getYamlDiff())
        .auditEventData(dto.getAuditEventData())
        .internalInfo(internalInfo)
        .build();
  }

  public static AuditEventDTO toDTO(AuditEvent auditEvent) {
    return AuditEventDTO.builder()
        .insertId(auditEvent.getInsertId())
        .resourceScope(auditEvent.getResourceScope())
        .httpRequestInfo(auditEvent.getHttpRequestInfo())
        .requestMetadata(auditEvent.getRequestMetadata())
        .timestamp(auditEvent.getTimestamp())
        .authenticationInfo(auditEvent.getAuthenticationInfo())
        .module(auditEvent.getModule())
        .resource(auditEvent.getResource())
        .action(auditEvent.getAction())
        .yamlDiff(auditEvent.getYamlDiff())
        .auditEventData(auditEvent.getAuditEventData())
        .build();
  }
}
