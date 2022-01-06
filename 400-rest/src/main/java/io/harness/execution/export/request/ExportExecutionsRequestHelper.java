/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.request;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExportExecutionsException;
import io.harness.execution.export.ExportExecutionsUtils;
import io.harness.execution.export.request.ExportExecutionsRequest.ExportExecutionsRequestKeys;
import io.harness.execution.export.request.ExportExecutionsRequest.Status;
import io.harness.execution.export.request.ExportExecutionsRequestSummary.ExportExecutionsRequestSummaryBuilder;

import software.wings.app.MainConfiguration;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Singleton
public class ExportExecutionsRequestHelper {
  public static final String EXPORT_EXECUTIONS_RESOURCE = "export-executions";
  public static final String STATUS_PATH = "status";
  public static final String DOWNLOAD_PATH = "download";

  private static final String URL_ENCODING = "UTF-8";

  @Inject private MainConfiguration mainConfiguration;

  public ExportExecutionsRequestSummary prepareSummary(@NotNull ExportExecutionsRequest request) {
    ExportExecutionsRequestSummaryBuilder summaryBuilder =
        ExportExecutionsRequestSummary.builder()
            .requestId(request.getUuid())
            .status(request.getStatus())
            .totalExecutions(request.getTotalExecutions())
            .triggeredAt(ExportExecutionsUtils.prepareZonedDateTime(request.getCreatedAt()));
    if (request.getStatus() == Status.QUEUED || request.getStatus() == Status.READY) {
      summaryBuilder.statusLink(prepareStatusLink(request.getAccountId(), request.getUuid()));
      summaryBuilder.downloadLink(prepareDownloadLink(request.getAccountId(), request.getUuid()));
      summaryBuilder.expiresAt(ExportExecutionsUtils.prepareZonedDateTime(request.getExpiresAt()));
    } else if (request.getStatus() == Status.FAILED) {
      summaryBuilder.errorMessage(request.getErrorMessage());
    }

    return summaryBuilder.build();
  }

  private String prepareStatusLink(@NotNull String accountId, @NotNull String requestId) {
    return prepareLink(accountId, requestId, STATUS_PATH);
  }

  public String prepareDownloadLink(@NotNull String accountId, @NotNull String requestId) {
    return prepareLink(accountId, requestId, DOWNLOAD_PATH);
  }

  @VisibleForTesting
  public String prepareLink(@NotNull String accountId, @NotNull String requestId, @NotNull String relativePath) {
    try {
      String apiUrl = mainConfiguration.getApiUrl() != null ? mainConfiguration.getApiUrl()
                                                            : mainConfiguration.getPortal().getUrl();
      URI uri = new URI(appendApi(apiUrl));
      uri = appendPath(uri, format("%s/%s/%s", EXPORT_EXECUTIONS_RESOURCE, relativePath, requestId));
      uri = addQueryParams(uri, ImmutableMap.of(ExportExecutionsRequestKeys.accountId, accountId));
      return uri.toString();
    } catch (Exception ex) {
      throw new ExportExecutionsException(
          format("Unable to generate %s link for export executions request", relativePath), ex);
    }
  }

  private static String appendApi(@NotNull String path) {
    path = appendSlash(path);
    if (path != null && path.endsWith("/api/")) {
      return path;
    }

    if (path == null) {
      return "/api/";
    }
    return format("%sapi/", path);
  }

  private static URI appendPath(@NotNull URI uri, @NotNull String relativePath) {
    return uri.resolve(format("%s%s", appendSlash(uri.getPath()), relativePath));
  }

  private static String appendSlash(String path) {
    if (path != null && !path.endsWith("/")) {
      path += "/";
    }
    return path;
  }

  private static URI addQueryParams(@NotNull URI uri, @NotEmpty Map<String, String> queryParams)
      throws UnsupportedEncodingException, URISyntaxException {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, String> entry : queryParams.entrySet()) {
      if (first) {
        first = false;
      } else {
        sb.append('&');
      }
      sb.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
    }

    String newQuery = uri.getQuery();
    if (newQuery == null) {
      newQuery = sb.toString();
    } else {
      newQuery += "&" + sb.toString();
    }

    return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), newQuery, uri.getFragment());
  }

  private static String encode(@NotNull String str) throws UnsupportedEncodingException {
    return URLEncoder.encode(str, URL_ENCODING);
  }
}
