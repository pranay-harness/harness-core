/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.service.impl;

import io.harness.ccm.views.dao.ViewCustomFieldDao;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewCondition;
import io.harness.ccm.views.entities.ViewCustomField;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewIdCondition;
import io.harness.ccm.views.entities.ViewRule;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.exception.InvalidRequestException;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ViewCustomFieldServiceImpl implements ViewCustomFieldService {
  @Inject private ViewCustomFieldDao viewCustomFieldDao;
  @Inject private CEViewService ceViewService;

  private static final String CUSTOM_FIELD_DUPLICATE_EXCEPTION = "Custom Field with given name already exists";
  private static final String CUSTOM_FIELD_IN_USE = "Custom Field in use, clean up all usages to delete the field";

  @Override
  public ViewCustomField save(ViewCustomField viewCustomField, BigQuery bigQuery, String cloudProviderTableName) {
    validateViewCustomField(viewCustomField, bigQuery, cloudProviderTableName, true, false);
    viewCustomFieldDao.save(viewCustomField);
    return viewCustomField;
  }

  @Override
  public ViewCustomField get(String uuid) {
    return viewCustomFieldDao.getById(uuid);
  }

  @Override
  public boolean validate(ViewCustomField viewCustomField, BigQuery bigQuery, String cloudProviderTableName) {
    return validateViewCustomField(viewCustomField, bigQuery, cloudProviderTableName, false, false);
  }

  @Override
  public List<ViewField> getCustomFields(String accountId) {
    List<ViewCustomField> viewCustomFields = viewCustomFieldDao.findByAccountId(accountId);
    return viewCustomFields.stream()
        .map(viewCustomField
            -> ViewField.builder()
                   .fieldId(viewCustomField.getUuid())
                   .identifier(ViewFieldIdentifier.CUSTOM)
                   .fieldName(viewCustomField.getName())
                   .identifierName(ViewFieldIdentifier.CUSTOM.getDisplayName())
                   .build())
        .collect(Collectors.toList());
  }

  @Override
  public List<ViewField> getCustomFieldsPerView(String viewId, String accountId) {
    List<ViewCustomField> viewCustomFields = viewCustomFieldDao.findByViewId(viewId, accountId);
    List<ViewField> viewFieldList = new ArrayList<>();
    for (ViewCustomField field : viewCustomFields) {
      viewFieldList.add(ViewField.builder()
                            .fieldId(field.getUuid())
                            .fieldName(field.getName())
                            .identifier(ViewFieldIdentifier.CUSTOM)
                            .identifierName(ViewFieldIdentifier.CUSTOM.getDisplayName())
                            .build());
    }
    return viewFieldList;
  }

  @Override
  public ViewCustomField update(ViewCustomField viewCustomField, BigQuery bigQuery, String cloudProviderTableName) {
    validateViewCustomField(viewCustomField, bigQuery, cloudProviderTableName, false, true);
    return viewCustomFieldDao.update(viewCustomField);
  }

  @Override
  public boolean delete(String uuid, String accountId, CEView ceView) {
    if (ceView.getViewRules() != null) {
      for (ViewRule rule : ceView.getViewRules()) {
        for (ViewCondition condition : rule.getViewConditions()) {
          ViewIdCondition viewIdCondition = (ViewIdCondition) condition;
          if (viewIdCondition.getViewField().getFieldId().equals(uuid)) {
            throw new InvalidRequestException(CUSTOM_FIELD_IN_USE);
          }
        }
      }
    }
    ceViewService.update(ceView);
    return viewCustomFieldDao.delete(uuid, accountId);
  }

  @Override
  public boolean deleteByViewId(String viewId, String accountId) {
    return viewCustomFieldDao.deleteByViewId(viewId, accountId);
  }

  public boolean validateViewCustomField(ViewCustomField viewCustomField, BigQuery bigQuery,
      String cloudProviderTableName, boolean isValidationOnSave, boolean isValidationOnUpdate) {
    if (isValidationOnSave) {
      ViewCustomField savedCustomField = viewCustomFieldDao.findByName(
          viewCustomField.getAccountId(), viewCustomField.getViewId(), viewCustomField.getName());
      if (null != savedCustomField) {
        throw new InvalidRequestException(CUSTOM_FIELD_DUPLICATE_EXCEPTION);
      }
    }

    if (isValidationOnSave || isValidationOnUpdate) {
      if (ImmutableSet
              .of("startTime", "cost", "gcpProduct", "gcpSkuId", "gcpSkuDescription", "gcpProjectId", "region", "zone",
                  "gcpBillingAccountId", "cloudProvider", "awsBlendedRate", "awsBlendedCost", "awsUnblendedRate",
                  "awsUnblendedCost", "awsServiceCode", "awsAvailabilityzone", "awsUsageaccountid", "awsInstanceType",
                  "awsUsagetype", "discount", "endtime", "accountid", "settingid", "instanceid", "instancetype",
                  "billingaccountid", "clusterid", "clustername", "appid", "serviceid", "envid", "cloudproviderid",
                  "parentinstanceid", "launchtype", "clustertype", "workloadname", "workloadtype", "namespace",
                  "cloudservicename", "taskid", "clustercloudprovider", "billingamount", "cpubillingamount",
                  "memorybillingamount", "idlecost", "cpuidlecost", "memoryidlecost", "usagedurationseconds",
                  "cpuunitseconds", "memorymbseconds", "maxcpuutilization", "maxmemoryutilization", "avgcpuutilization",
                  "avgmemoryutilization", "systemcost", "cpusystemcost", "memorysystemcost", "actualidlecost",
                  "cpuactualidlecost", "memoryactualidlecost", "instancename", "cpurequest", "memoryrequest",
                  "cpulimit", "memorylimit", "maxcpuutilizationvalue", "maxmemoryutilizationvalue",
                  "avgcpuutilizationvalue", "avgmemoryutilizationvalue", "networkcost", "pricingsource", "product",
                  "labels")
              .contains(viewCustomField.getName())) {
        throw new InvalidRequestException(CUSTOM_FIELD_DUPLICATE_EXCEPTION);
      }
    }

    SelectQuery selectQuery = new SelectQuery();
    selectQuery.addCustomFromTable(cloudProviderTableName);
    selectQuery.addCustomColumns(new CustomSql(viewCustomField.getSqlFormula()));

    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(selectQuery.toString()).setDryRun(true).setUseQueryCache(false).build();

    try {
      bigQuery.create(JobInfo.of(queryConfig));
    } catch (BigQueryException e) {
      throw new InvalidRequestException(e.getMessage());
    }

    return true;
  }
}
