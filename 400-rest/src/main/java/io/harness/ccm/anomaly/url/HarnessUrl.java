/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.anomaly.url;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.anomaly.utility.AnomalyUtility;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;

@UtilityClass
@Slf4j
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class HarnessUrl {
  static final String SETTING_PATH = "account/%1$s/continuous-efficiency/settings";
  static final String PATH_TEMPLATE = "/account/%1$s/continuous-efficiency/%2$s/insights";
  static final String OVERVIEW_PATH = "/account/%1$s/continuous-efficiency/overview";

  public static String getOverviewPageUrl(String accountId, String baseUrl, Instant date) {
    try {
      URIBuilder uriBuilder = getBaseUrl(baseUrl).setPath(String.format(OVERVIEW_PATH, accountId));
      addBaseParams(uriBuilder);
      addDateParams(uriBuilder, date);
      return addHash(uriBuilder.build()).toString();
    } catch (URISyntaxException | UnsupportedEncodingException exception) {
      log.error("", exception);
    }
    return null;
  }

  public static String getCeSlackCommunicationSettings(String accountId, String baseUrl) {
    try {
      URIBuilder uriBuilder = getBaseUrl(baseUrl).setPath(String.format(SETTING_PATH, accountId));
      addToQuery(uriBuilder, UrlParams.CURRENT_TAB, "slackReport");
      addToQuery(uriBuilder, UrlParams.FILTER, "all");
      addToQuery(uriBuilder, UrlParams.SELECTED_VIEW, "COMMUNICATION");
      return addHash(uriBuilder.build()).toString();
    } catch (URISyntaxException exception) {
      log.error("", exception);
    }
    return null;
  }

  public String getK8sUrl(AnomalyEntity anomaly, String baseUrl) {
    try {
      URIBuilder uriBuilder = getBaseUrl(baseUrl).setPath(getPath(anomaly));
      addBaseParams(uriBuilder);
      addDateParams(uriBuilder, anomaly.getAnomalyTime());
      addEntityParams(uriBuilder, anomaly);
      addGroupBY(uriBuilder, anomaly);
      return addHash(uriBuilder.build()).toString();
    } catch (UnsupportedEncodingException | URISyntaxException exception) {
      log.error("", exception);
    }
    return null;
  }

  private static void addGroupBY(URIBuilder uriBuilder, AnomalyEntity anomaly) throws UnsupportedEncodingException {
    switch (anomaly.getEntityType()) {
      case CLUSTER:
        addToQuery(uriBuilder, UrlParams.GROUP_BY, UrlGroupBys.CLUSTER.getValue());
        break;
      case NAMESPACE:
        addToQuery(uriBuilder, UrlParams.GROUP_BY, UrlGroupBys.NAMESPACE.getValue());
        break;
      case WORKLOAD:
        addToQuery(uriBuilder, UrlParams.GROUP_BY, UrlGroupBys.WORKLOAD.getValue());
        break;
      case GCP_PRODUCT:
        addToQuery(uriBuilder, UrlParams.GROUP_BY, UrlGroupBys.GCP_PRODUCT.getValue());
        break;
      case GCP_PROJECT:
        addToQuery(uriBuilder, UrlParams.GROUP_BY, UrlGroupBys.GCP_PROJECT.getValue());
        break;
      case GCP_SKU_ID:
        addToQuery(uriBuilder, UrlParams.GROUP_BY, UrlGroupBys.GCP_SKU_ID.getValue());
        break;
      case AWS_ACCOUNT:
        addToQuery(uriBuilder, UrlParams.GROUP_BY, UrlGroupBys.AWS_ACCOUNT.getValue());
        break;
      case AWS_SERVICE:
        addToQuery(uriBuilder, UrlParams.GROUP_BY, UrlGroupBys.AWS_SERVICE.getValue());
        break;
      default:
        log.error("Group by not supported in harnessUrl");
        break;
    }
  }

  private static void addEntityParams(URIBuilder uriBuilder, AnomalyEntity anomaly) {
    if (EmptyPredicate.isNotEmpty(anomaly.getClusterId())) {
      addToQuery(uriBuilder, UrlParams.CURRENT_VIEW, "TOTAL_COST");
      addToQuery(uriBuilder, UrlParams.FILTER_ON, "true");
      addToQuery(uriBuilder, UrlParams.SHOW_UNALLOCATED, "false");
      addToQuery(uriBuilder, UrlParams.GCP_DISCOUNTS, "false");
      addToQuery(uriBuilder, UrlParams.CLUSTER_ID, encodeValue(anomaly.getClusterId()));
    }
    if (EmptyPredicate.isNotEmpty(anomaly.getNamespace())) {
      addToQuery(uriBuilder, UrlParams.NAMESPACE, encodeValue(anomaly.getNamespace()));
    }

    if (EmptyPredicate.isNotEmpty(anomaly.getGcpProject())) {
      addToQuery(uriBuilder, UrlParams.CURRENT_VIEW, "GCP_COST");
      addToQuery(uriBuilder, UrlParams.FILTER_ON, "true");
      addToQuery(uriBuilder, UrlParams.SHOW_UNALLOCATED, "false");
      addToQuery(uriBuilder, UrlParams.GCP_DISCOUNTS, "false");
      addToQuery(uriBuilder, UrlParams.GCP_PROJECT, encodeValue(anomaly.getGcpProject()));
    }
    if (EmptyPredicate.isNotEmpty(anomaly.getGcpProduct())) {
      addToQuery(uriBuilder, UrlParams.GCP_PRODUCT, encodeValue(anomaly.getGcpProduct()));
    }
    if (EmptyPredicate.isNotEmpty(anomaly.getGcpSKUDescription())) {
      addToQuery(uriBuilder, UrlParams.GCP_SKU, encodeValue(anomaly.getGcpSKUDescription()));
    }

    if (EmptyPredicate.isNotEmpty(anomaly.getAwsAccount())) {
      addToQuery(uriBuilder, UrlParams.CURRENT_VIEW, "UNBLENDED_COST");
      addToQuery(uriBuilder, UrlParams.FILTER_ON, "true");
      addToQuery(uriBuilder, UrlParams.SHOW_UNALLOCATED, "false");
      addToQuery(uriBuilder, UrlParams.GCP_DISCOUNTS, "false");
      addToQuery(uriBuilder, UrlParams.AWS_ACCOUNT, encodeValue(anomaly.getAwsAccount()));
    }
    if (EmptyPredicate.isNotEmpty(anomaly.getAwsService())) {
      addToQuery(uriBuilder, UrlParams.AWS_SERVICE, encodeValue(anomaly.getAwsService()));
    }
  }

  // ------ Base Methods ------------

  private static void addBaseParams(URIBuilder uriBuilder) throws UnsupportedEncodingException {
    addToQuery(uriBuilder, UrlParams.AGGREGATION_TYPE, encodeValue("DAY"));
    addToQuery(uriBuilder, UrlParams.CHART_TYPE, encodeValue("column"));
    addToQuery(uriBuilder, UrlParams.UTILIZATION_AGGREGATION, encodeValue("Average"));
    addToQuery(uriBuilder, UrlParams.SHOW_OTHERS, "false");
  }

  private static void addDateParams(URIBuilder uriBuilder, Instant date) throws UnsupportedEncodingException {
    addToQuery(uriBuilder, UrlParams.FROM_DATE,
        AnomalyUtility.convertInstantToDate(date.minus(4, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS)));
    addToQuery(uriBuilder, UrlParams.TO_DATE,
        AnomalyUtility.convertInstantToDate(date.plus(4, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS)));
  }

  private URIBuilder getBaseUrl(String baseUrl) throws URISyntaxException {
    return new URIBuilder(baseUrl);
  }

  private static String getPath(AnomalyEntity anomaly) {
    String path;
    switch (anomaly.getEntityType()) {
      case CLUSTER:
      case NAMESPACE:
      case WORKLOAD:
        path = String.format(PATH_TEMPLATE, anomaly.getAccountId(), "cluster");
        break;
      case GCP_PROJECT:
      case GCP_PRODUCT:
      case GCP_SKU_ID:
        path = String.format(PATH_TEMPLATE, anomaly.getAccountId(), "gcp");
        break;
      case AWS_ACCOUNT:
      case AWS_SERVICE:
      case AWS_USAGE_TYPE:
      case AWS_INSTANCE_TYPE:
        path = String.format(PATH_TEMPLATE, anomaly.getAccountId(), "aws");
        break;
      default:
        throw new InvalidArgumentsException("Entity Type Not supported ");
    }
    return path;
  }

  //-------- Util Methods -------------

  private static URI addHash(URI url) throws URISyntaxException {
    return new URI(url.getScheme(), url.getHost(), "/", url.getPath() + "?" + url.getQuery());
  }

  private static void addToQuery(URIBuilder urlBuilder, UrlParams param, String value) {
    urlBuilder.addParameter(param.getParam(), encodeValue(value));
  }

  private static String encodeValue(String value) {
    return value;
  }
}
