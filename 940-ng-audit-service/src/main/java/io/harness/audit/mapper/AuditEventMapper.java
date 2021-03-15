package io.harness.audit.mapper;

import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.entities.AuditEvent;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AuditEventMapper {
  public static AuditEvent fromDTO(AuditEventDTO dto) {
    return AuditEvent.builder()
        .id(dto.getId())
        .accountIdentifier(dto.getAccountIdentifier())
        .orgIdentifier(dto.getOrgIdentifier())
        .projectIdentifier(dto.getProjectIdentifier())
        .httpRequestInfo(dto.getHttpRequestInfo())
        .requestMetadata(dto.getRequestMetadata())
        .timestamp(dto.getTimestamp())
        .authenticationInfo(dto.getAuthenticationInfo())
        .moduleType(dto.getModuleType())
        .resource(dto.getResource())
        .action(dto.getAction())
        .yamlDiff(dto.getYamlDiff())
        .auditEventData(dto.getAuditEventData())
        .additionalInfo(dto.getAdditionalInfo())
        .build();
  }

  public static AuditEventDTO toDTO(AuditEvent auditEvent) {
    return AuditEventDTO.builder()
        .id(auditEvent.getId())
        .accountIdentifier(auditEvent.getAccountIdentifier())
        .orgIdentifier(auditEvent.getOrgIdentifier())
        .projectIdentifier(auditEvent.getProjectIdentifier())
        .httpRequestInfo(auditEvent.getHttpRequestInfo())
        .requestMetadata(auditEvent.getRequestMetadata())
        .timestamp(auditEvent.getTimestamp())
        .authenticationInfo(auditEvent.getAuthenticationInfo())
        .moduleType(auditEvent.getModuleType())
        .resource(auditEvent.getResource())
        .action(auditEvent.getAction())
        .yamlDiff(auditEvent.getYamlDiff())
        .auditEventData(auditEvent.getAuditEventData())
        .additionalInfo(auditEvent.getAdditionalInfo())
        .build();
  }
}
