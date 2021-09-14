/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.testframework.restutils;

import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeaderYamlResponse;

import javax.ws.rs.core.GenericType;

public class AuditTrailUtils {
  public static RestResponse<PageResponse<AuditHeader>> getAuditTrailInfo(
      String bearerToken, String accountId, String filterString) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .queryParam("limits", 40)
        .queryParam("offset", 0)
        .queryParam("filter", filterString)
        .get("/audits/filter")
        .as(new GenericType<RestResponse<PageResponse<AuditHeader>>>() {}.getType());
  }

  public static RestResponse<AuditHeaderYamlResponse> getYamlResponse(
      String bearerToken, String auditHeaderId, String entityId, String accountId) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("entityId", entityId)
        .queryParam("accountId", accountId)
        .get("/audits/" + auditHeaderId + "/yamldetails")
        .as(new GenericType<RestResponse<AuditHeaderYamlResponse>>() {}.getType());
  }
}
