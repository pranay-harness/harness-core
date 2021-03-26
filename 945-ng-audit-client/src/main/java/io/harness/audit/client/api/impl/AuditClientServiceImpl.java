package io.harness.audit.client.api.impl;

import static io.harness.audit.beans.Principal.fromSecurityPrincipal;
import static io.harness.remote.client.NGRestUtils.getResponse;
import static io.harness.request.RequestContextData.REQUEST_CONTEXT;
import static io.harness.security.PrincipalContextData.PRINCIPAL_CONTEXT;

import io.harness.audit.beans.AuditEntry;
import io.harness.audit.beans.AuditEventDTO;
import io.harness.audit.beans.AuditEventDTO.AuditEventDTOBuilder;
import io.harness.audit.beans.AuthenticationInfo;
import io.harness.audit.client.api.AuditClientService;
import io.harness.audit.client.remote.AuditClient;
import io.harness.context.GlobalContext;
import io.harness.request.HttpRequestInfo;
import io.harness.request.RequestContextData;
import io.harness.request.RequestMetadata;
import io.harness.security.PrincipalContextData;
import io.harness.security.dto.Principal;

import com.google.inject.Inject;
import java.time.Instant;

public class AuditClientServiceImpl implements AuditClientService {
  @Inject private AuditClient auditClient;

  public boolean publishAudit(AuditEntry auditEntry, GlobalContext globalContext) {
    HttpRequestInfo httpRequestInfo = null;
    RequestMetadata requestMetadata = null;
    Principal principal = null;
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
    AuditEventDTOBuilder auditEventDTOBuilder = AuditEventDTO.builder()
                                                    .resource(auditEntry.getResource())
                                                    .action(auditEntry.getAction())
                                                    .resourceScope(auditEntry.getResourceScope())
                                                    .insertId(auditEntry.getInsertId())
                                                    .module(auditEntry.getModule())
                                                    .timestamp(Instant.ofEpochMilli(auditEntry.getTimestamp()));

    if (principal != null) {
      auditEventDTOBuilder.authenticationInfo(
          AuthenticationInfo.builder().principal(fromSecurityPrincipal(principal)).build());
    }
    if (requestMetadata != null) {
      auditEventDTOBuilder.requestMetadata(requestMetadata);
    }
    if (httpRequestInfo != null) {
      auditEventDTOBuilder.httpRequestInfo(httpRequestInfo);
    }
    return getResponse(auditClient.createAudit(auditEventDTOBuilder.build()));
  }
}
