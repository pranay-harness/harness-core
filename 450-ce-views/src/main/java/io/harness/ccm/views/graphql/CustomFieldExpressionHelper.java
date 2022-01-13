/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.utils.ViewFieldUtils;

import java.util.HashMap;
import java.util.List;

@OwnedBy(CE)
public class CustomFieldExpressionHelper {
  public static HashMap<String, ViewField> getViewFieldsHashMap() {
    HashMap<String, ViewField> viewFieldHashMap = new HashMap<>();

    List<QLCEViewField> commonFields = ViewFieldUtils.getCommonFields();
    viewFieldHashMap.put("COMMON.Region",
        ViewField.builder()
            .fieldId(commonFields.get(0).getFieldId())
            .fieldName(commonFields.get(0).getFieldId())
            .identifier(ViewFieldIdentifier.COMMON)
            .build());
    viewFieldHashMap.put("COMMON.Product",
        ViewField.builder()
            .fieldId(commonFields.get(1).getFieldId())
            .fieldName(commonFields.get(1).getFieldId())
            .identifier(ViewFieldIdentifier.COMMON)
            .build());

    List<QLCEViewField> awsFields = ViewFieldUtils.getAwsFields();
    viewFieldHashMap.put("AWS.Service",
        ViewField.builder()
            .fieldId(awsFields.get(0).getFieldId())
            .fieldName(awsFields.get(0).getFieldId())
            .identifier(ViewFieldIdentifier.AWS)
            .build());
    viewFieldHashMap.put("AWS.Account",
        ViewField.builder()
            .fieldId(awsFields.get(1).getFieldId())
            .fieldName(awsFields.get(1).getFieldId())
            .identifier(ViewFieldIdentifier.AWS)
            .build());
    viewFieldHashMap.put("AWS.Instance Type",
        ViewField.builder()
            .fieldId(awsFields.get(2).getFieldId())
            .fieldName(awsFields.get(2).getFieldId())
            .identifier(ViewFieldIdentifier.AWS)
            .build());
    viewFieldHashMap.put("AWS.Usage Type",
        ViewField.builder()
            .fieldId(awsFields.get(3).getFieldId())
            .fieldName(awsFields.get(3).getFieldId())
            .identifier(ViewFieldIdentifier.AWS)
            .build());

    List<QLCEViewField> gcpFields = ViewFieldUtils.getGcpFields();
    viewFieldHashMap.put("GCP.Product",
        ViewField.builder()
            .fieldId(gcpFields.get(0).getFieldId())
            .fieldName(gcpFields.get(0).getFieldId())
            .identifier(ViewFieldIdentifier.GCP)
            .build());
    viewFieldHashMap.put("GCP.Project",
        ViewField.builder()
            .fieldId(gcpFields.get(1).getFieldId())
            .fieldName(gcpFields.get(1).getFieldId())
            .identifier(ViewFieldIdentifier.GCP)
            .build());
    viewFieldHashMap.put("GCP.SKUs",
        ViewField.builder()
            .fieldId(gcpFields.get(2).getFieldId())
            .fieldName(gcpFields.get(2).getFieldId())
            .identifier(ViewFieldIdentifier.GCP)
            .build());

    List<QLCEViewField> azureFields = ViewFieldUtils.getAzureFields();
    List<QLCEViewField> variableAzureFields = ViewFieldUtils.getVariableAzureFields();
    viewFieldHashMap.put("AZURE.Subscription id",
        ViewField.builder()
            .fieldId(azureFields.get(0).getFieldId())
            .fieldName(azureFields.get(0).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());
    viewFieldHashMap.put("AZURE.Meter",
        ViewField.builder()
            .fieldId(azureFields.get(1).getFieldId())
            .fieldName(azureFields.get(1).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());
    viewFieldHashMap.put("AZURE.Meter category",
        ViewField.builder()
            .fieldId(azureFields.get(2).getFieldId())
            .fieldName(azureFields.get(2).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());
    viewFieldHashMap.put("AZURE.Meter subcategory",
        ViewField.builder()
            .fieldId(azureFields.get(3).getFieldId())
            .fieldName(azureFields.get(3).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());
    viewFieldHashMap.put("AZURE.Resource guid",
        ViewField.builder()
            .fieldId(azureFields.get(4).getFieldId())
            .fieldName(azureFields.get(4).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());
    viewFieldHashMap.put("AZURE.Resource group name",
        ViewField.builder()
            .fieldId(azureFields.get(5).getFieldId())
            .fieldName(azureFields.get(5).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());
    viewFieldHashMap.put("AZURE.Resource type",
        ViewField.builder()
            .fieldId(azureFields.get(6).getFieldId())
            .fieldName(azureFields.get(6).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());
    viewFieldHashMap.put("AZURE.Resource",
        ViewField.builder()
            .fieldId(azureFields.get(7).getFieldId())
            .fieldName(azureFields.get(7).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());
    viewFieldHashMap.put("AZURE.Service name",
        ViewField.builder()
            .fieldId(azureFields.get(8).getFieldId())
            .fieldName(azureFields.get(8).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());
    viewFieldHashMap.put("AZURE.Service tier",
        ViewField.builder()
            .fieldId(azureFields.get(9).getFieldId())
            .fieldName(azureFields.get(9).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());
    viewFieldHashMap.put("AZURE.Instance id",
        ViewField.builder()
            .fieldId(azureFields.get(10).getFieldId())
            .fieldName(azureFields.get(10).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());
    viewFieldHashMap.put("AZURE.Subscription name",
        ViewField.builder()
            .fieldId(variableAzureFields.get(0).getFieldId())
            .fieldName(variableAzureFields.get(0).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());
    viewFieldHashMap.put("AZURE.Publisher name",
        ViewField.builder()
            .fieldId(variableAzureFields.get(1).getFieldId())
            .fieldName(variableAzureFields.get(1).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());
    viewFieldHashMap.put("AZURE.Publisher type",
        ViewField.builder()
            .fieldId(variableAzureFields.get(2).getFieldId())
            .fieldName(variableAzureFields.get(2).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());
    viewFieldHashMap.put("AZURE.Reservation id",
        ViewField.builder()
            .fieldId(variableAzureFields.get(3).getFieldId())
            .fieldName(variableAzureFields.get(3).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());
    viewFieldHashMap.put("AZURE.Reservation name",
        ViewField.builder()
            .fieldId(variableAzureFields.get(4).getFieldId())
            .fieldName(variableAzureFields.get(4).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());
    viewFieldHashMap.put("AZURE.Frequency",
        ViewField.builder()
            .fieldId(variableAzureFields.get(5).getFieldId())
            .fieldName(variableAzureFields.get(5).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .build());

    List<QLCEViewField> clusterFields = ViewFieldUtils.getClusterFields();
    viewFieldHashMap.put("CLUSTER.Cluster Name",
        ViewField.builder()
            .fieldId(clusterFields.get(0).getFieldId())
            .fieldName(clusterFields.get(0).getFieldId())
            .identifier(ViewFieldIdentifier.CLUSTER)
            .build());
    viewFieldHashMap.put("CLUSTER.Namespace",
        ViewField.builder()
            .fieldId(clusterFields.get(1).getFieldId())
            .fieldName(clusterFields.get(1).getFieldId())
            .identifier(ViewFieldIdentifier.CLUSTER)
            .build());
    viewFieldHashMap.put("CLUSTER.Workload",
        ViewField.builder()
            .fieldId(clusterFields.get(2).getFieldId())
            .fieldName(clusterFields.get(2).getFieldId())
            .identifier(ViewFieldIdentifier.CLUSTER)
            .build());
    viewFieldHashMap.put("CLUSTER.Application",
        ViewField.builder()
            .fieldId(clusterFields.get(3).getFieldId())
            .fieldName(clusterFields.get(3).getFieldId())
            .identifier(ViewFieldIdentifier.CLUSTER)
            .build());
    viewFieldHashMap.put("CLUSTER.Environment",
        ViewField.builder()
            .fieldId(clusterFields.get(4).getFieldId())
            .fieldName(clusterFields.get(4).getFieldId())
            .identifier(ViewFieldIdentifier.CLUSTER)
            .build());
    viewFieldHashMap.put("CLUSTER.Service",
        ViewField.builder()
            .fieldId(clusterFields.get(5).getFieldId())
            .fieldName(clusterFields.get(5).getFieldId())
            .identifier(ViewFieldIdentifier.CLUSTER)
            .build());

    return viewFieldHashMap;
  }
}
