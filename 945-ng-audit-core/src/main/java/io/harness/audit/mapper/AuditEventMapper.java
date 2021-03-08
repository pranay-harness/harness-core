package io.harness.audit.mapper;

import io.harness.audit.AuditEvent;
import io.harness.audit.beans.AuditEventDTO;

import com.google.inject.Inject;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class AuditEventMapper {
  private final Map<String, AuditEventDataEntityToDTOMapper> auditEventDataEntityToDTOMapper;
  private final Map<String, AuditEventDataDTOToEntityMapper> auditEventDataDTOtoEntityMapper;

  public AuditEvent fromDTO(AuditEventDTO dto) {
    return AuditEvent.builder()
        .id(dto.getId())
        .accountIdentifier(dto.getAccountIdentifier())
        .orgIdentifier(dto.getOrgIdentifier())
        .projectIdentifier(dto.getProjectIdentifier())
        .httpRequestInfo(dto.getHttpRequestInfo())
        .requestMetadata(dto.getRequestMetadata())
        .authenticationInfo(dto.getAuthenticationInfo())
        .moduleType(dto.getModuleType())
        .resource(dto.getResource())
        .action(dto.getAction())
        .yamlDiff(dto.getYamlDiff())
        .auditEventData(dto.getAuditEventData() != null
                ? null
                : auditEventDataDTOtoEntityMapper.get(dto.getAuditEventData().getType())
                      .fromDTO(dto.getAuditEventData()))
        .additionalInfo(dto.getAdditionalInfo())
        .build();
  }

  public AuditEventDTO toDTO(AuditEvent auditEvent) {
    return AuditEventDTO.builder()
        .id(auditEvent.getId())
        .accountIdentifier(auditEvent.getAccountIdentifier())
        .orgIdentifier(auditEvent.getOrgIdentifier())
        .projectIdentifier(auditEvent.getProjectIdentifier())
        .httpRequestInfo(auditEvent.getHttpRequestInfo())
        .requestMetadata(auditEvent.getRequestMetadata())
        .authenticationInfo(auditEvent.getAuthenticationInfo())
        .moduleType(auditEvent.getModuleType())
        .resource(auditEvent.getResource())
        .action(auditEvent.getAction())
        .yamlDiff(auditEvent.getYamlDiff())
        .auditEventData(auditEvent.getAuditEventData() == null
                ? null
                : auditEventDataEntityToDTOMapper.get(auditEvent.getAuditEventData().getType())
                      .toDTO(auditEvent.getAuditEventData()))
        .additionalInfo(auditEvent.getAdditionalInfo())
        .build();
  }
}
