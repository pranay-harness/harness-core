package io.harness.audit.mapper;

import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.entities.AuditEvent;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AuditEventMapper {
  public static AuditEvent fromDTO(AuditEventDTO dto) {
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
        .additionalInfo(dto.getAdditionalInfo())
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
        .additionalInfo(auditEvent.getAdditionalInfo())
        .build();
  }
}
