package io.harness.audit.client.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.AuditCommonConstants.CORRELATION_ID;
import static io.harness.audit.beans.AuthenticationInfoDTO.fromSecurityPrincipal;
import static io.harness.context.MdcGlobalContextData.MDC_ID;
import static io.harness.ng.core.CorrelationContext.getCorrelationIdKey;
import static io.harness.remote.client.NGRestUtils.getResponse;
import static io.harness.request.RequestContextData.REQUEST_CONTEXT;
import static io.harness.security.PrincipalContextData.PRINCIPAL_CONTEXT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.beans.AuditEventDTO.AuditEventDTOBuilder;
import io.harness.audit.beans.YamlDiffRecordDTO;
import io.harness.audit.client.api.AuditClientService;
import io.harness.audit.client.remote.AuditClient;
import io.harness.context.GlobalContext;
import io.harness.context.MdcGlobalContextData;
import io.harness.request.HttpRequestInfo;
import io.harness.request.RequestContextData;
import io.harness.request.RequestMetadata;
import io.harness.security.PrincipalContextData;
import io.harness.security.dto.Principal;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

@OwnedBy(PL)
public class AuditClientServiceImpl implements AuditClientService {
  @Inject private AuditClient auditClient;

  public boolean publishAudit(AuditEntry auditEntry, GlobalContext globalContext) {
    HttpRequestInfo httpRequestInfo = null;
    RequestMetadata requestMetadata = null;
    Principal principal = null;
    String correlationId = null;
    if (globalContext.get(REQUEST_CONTEXT) instanceof RequestContextData
        && ((RequestContextData) globalContext.get(REQUEST_CONTEXT)).getRequestContext() != null) {
      httpRequestInfo =
          ((RequestContextData) globalContext.get(REQUEST_CONTEXT)).getRequestContext().getHttpRequestInfo();
      requestMetadata =
          ((RequestContextData) globalContext.get(REQUEST_CONTEXT)).getRequestContext().getRequestMetadata();
    }
    if (globalContext.get(PRINCIPAL_CONTEXT) instanceof PrincipalContextData) {
      principal = ((PrincipalContextData) globalContext.get(PRINCIPAL_CONTEXT)).getPrincipal();
    }
    if (globalContext.get(MDC_ID) instanceof MdcGlobalContextData
        && ((MdcGlobalContextData) globalContext.get(MDC_ID)).getMap() != null) {
      correlationId = ((MdcGlobalContextData) globalContext.get(MDC_ID)).getMap().get(getCorrelationIdKey());
    }

    YamlDiffRecordDTO yamlDiffRecordDTO =
        YamlDiffRecordDTO.builder().newYaml(auditEntry.getNewYaml()).oldYaml(auditEntry.getOldYaml()).build();

    AuditEventDTOBuilder auditEventDTOBuilder = AuditEventDTO.builder()
                                                    .resource(auditEntry.getResource())
                                                    .action(auditEntry.getAction())
                                                    .resourceScope(auditEntry.getResourceScope())
                                                    .insertId(auditEntry.getInsertId())
                                                    .module(auditEntry.getModule())
                                                    .auditEventData(auditEntry.getAuditEventData())
                                                    .environment(auditEntry.getEnvironment())
                                                    .yamlDiffRecordDTO(yamlDiffRecordDTO)
                                                    .timestamp(auditEntry.getTimestamp());
    if (principal != null) {
      auditEventDTOBuilder.authenticationInfo(fromSecurityPrincipal(principal));
    }
    if (requestMetadata != null) {
      auditEventDTOBuilder.requestMetadata(requestMetadata);
    }
    if (httpRequestInfo != null) {
      auditEventDTOBuilder.httpRequestInfo(httpRequestInfo);
    }
    if (correlationId != null) {
      auditEventDTOBuilder.internalInfo(ImmutableMap.of(CORRELATION_ID, correlationId));
    }
    return getResponse(auditClient.createAudit(auditEventDTOBuilder.build()));
  }
}
