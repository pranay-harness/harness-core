package io.harness.ccm.views.graphql;

import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.utils.ViewFieldUtils;

import java.util.HashMap;
import java.util.List;

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
