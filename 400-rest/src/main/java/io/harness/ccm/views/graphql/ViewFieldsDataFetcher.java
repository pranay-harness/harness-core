package io.harness.ccm.views.graphql;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.ccm.setup.CEMetadataRecordDao;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.ccm.views.utils.ViewFieldUtils;

import software.wings.beans.ce.CEMetadataRecord;
import software.wings.graphql.datafetcher.AbstractFieldsDataFetcher;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ViewFieldsDataFetcher extends AbstractFieldsDataFetcher<QLCEViewFieldsData, QLCEViewFilterWrapper> {
  @Inject private ViewCustomFieldService viewCustomFieldService;
  @Inject private CEMetadataRecordDao metadataRecordDao;
  @Inject private CEViewService ceViewService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLCEViewFieldsData fetch(String accountId, List<QLCEViewFilterWrapper> filters) {
    List<ViewField> customFields = new ArrayList<>();
    Optional<QLCEViewFilterWrapper> viewMetadataFilter = getViewMetadataFilter(filters);
    boolean isExplorerQuery = false;
    String viewId = null;
    if (viewMetadataFilter.isPresent()) {
      QLCEViewMetadataFilter metadataFilter = viewMetadataFilter.get().getViewMetadataFilter();
      isExplorerQuery = !metadataFilter.isPreview();
      viewId = metadataFilter.getViewId();
      customFields = viewCustomFieldService.getCustomFieldsPerView(viewId);
    }

    List<QLCEViewFieldIdentifierData> fieldIdentifierData = new ArrayList<>();
    fieldIdentifierData.add(getViewField(ViewFieldUtils.getCommonFields(), ViewFieldIdentifier.COMMON));
    fieldIdentifierData.add(getViewCustomField(customFields));

    Set<ViewFieldIdentifier> viewFieldIdentifierSetFromCustomFields = new HashSet<>();
    for (ViewField customField : customFields) {
      List<ViewField> customFieldViewFields = viewCustomFieldService.get(customField.getFieldId()).getViewFields();
      for (ViewField field : customFieldViewFields) {
        if (field.getIdentifier() == ViewFieldIdentifier.LABEL) {
          for (QLCEViewFieldIdentifierData viewFieldIdentifierData :
              getFieldIdentifierDataFromCEMetadataRecord(accountId)) {
            viewFieldIdentifierSetFromCustomFields.add(viewFieldIdentifierData.getIdentifier());
          }
        } else {
          viewFieldIdentifierSetFromCustomFields.add(field.getIdentifier());
        }
      }
    }

    if (isExplorerQuery) {
      CEView ceView = ceViewService.get(viewId);
      if (ceView.getDataSources() != null && isNotEmpty(ceView.getDataSources())) {
        for (ViewFieldIdentifier viewFieldIdentifier : viewFieldIdentifierSetFromCustomFields) {
          if (viewFieldIdentifier == ViewFieldIdentifier.AWS) {
            fieldIdentifierData.add(getViewField(ViewFieldUtils.getAwsFields(), viewFieldIdentifier));
          } else if (viewFieldIdentifier == ViewFieldIdentifier.GCP) {
            fieldIdentifierData.add(getViewField(ViewFieldUtils.getGcpFields(), viewFieldIdentifier));
          } else if (viewFieldIdentifier == ViewFieldIdentifier.CLUSTER) {
            fieldIdentifierData.add(getViewField(ViewFieldUtils.getClusterFields(), viewFieldIdentifier));
          }
        }

        for (ViewFieldIdentifier viewFieldIdentifier : ceView.getDataSources()) {
          if (viewFieldIdentifier == ViewFieldIdentifier.AWS
              && !viewFieldIdentifierSetFromCustomFields.contains(ViewFieldIdentifier.AWS)) {
            fieldIdentifierData.add(getViewField(ViewFieldUtils.getAwsFields(), viewFieldIdentifier));
          } else if (viewFieldIdentifier == ViewFieldIdentifier.GCP
              && !viewFieldIdentifierSetFromCustomFields.contains(ViewFieldIdentifier.GCP)) {
            fieldIdentifierData.add(getViewField(ViewFieldUtils.getGcpFields(), viewFieldIdentifier));
          } else if (viewFieldIdentifier == ViewFieldIdentifier.CLUSTER
              && !viewFieldIdentifierSetFromCustomFields.contains(ViewFieldIdentifier.CLUSTER)) {
            fieldIdentifierData.add(getViewField(ViewFieldUtils.getClusterFields(), viewFieldIdentifier));
          }
        }
      } else {
        fieldIdentifierData.addAll(getFieldIdentifierDataFromCEMetadataRecord(accountId));
      }
    } else {
      fieldIdentifierData.addAll(getFieldIdentifierDataFromCEMetadataRecord(accountId));
    }
    return QLCEViewFieldsData.builder().fieldIdentifierData(fieldIdentifierData).build();
  }

  private List<QLCEViewFieldIdentifierData> getFieldIdentifierDataFromCEMetadataRecord(String accountId) {
    List<QLCEViewFieldIdentifierData> fieldIdentifierData = new ArrayList<>();
    CEMetadataRecord ceMetadataRecord = metadataRecordDao.getByAccountId(accountId);
    Boolean clusterDataConfigured = true;
    Boolean awsConnectorConfigured = true;
    Boolean gcpConnectorConfigured = true;

    if (ceMetadataRecord != null) {
      clusterDataConfigured = ceMetadataRecord.getClusterDataConfigured();
      awsConnectorConfigured = ceMetadataRecord.getAwsConnectorConfigured();
      gcpConnectorConfigured = ceMetadataRecord.getGcpConnectorConfigured();
    }

    if (clusterDataConfigured == null || clusterDataConfigured) {
      fieldIdentifierData.add(getViewField(ViewFieldUtils.getClusterFields(), ViewFieldIdentifier.CLUSTER));
    }
    if (awsConnectorConfigured == null || awsConnectorConfigured) {
      fieldIdentifierData.add(getViewField(ViewFieldUtils.getAwsFields(), ViewFieldIdentifier.AWS));
    }
    if (gcpConnectorConfigured == null || gcpConnectorConfigured) {
      fieldIdentifierData.add(getViewField(ViewFieldUtils.getGcpFields(), ViewFieldIdentifier.GCP));
    }
    return fieldIdentifierData;
  }

  private QLCEViewFieldIdentifierData getViewField(
      List<QLCEViewField> ceViewFieldList, ViewFieldIdentifier viewFieldIdentifier) {
    return QLCEViewFieldIdentifierData.builder()
        .identifier(viewFieldIdentifier)
        .identifierName(viewFieldIdentifier.getDisplayName())
        .values(ceViewFieldList)
        .build();
  }

  private QLCEViewFieldIdentifierData getViewCustomField(List<ViewField> customFields) {
    List<QLCEViewField> ceViewFieldList = customFields.stream()
                                              .map(field
                                                  -> QLCEViewField.builder()
                                                         .fieldId(field.getFieldId())
                                                         .fieldName(field.getFieldName())
                                                         .identifier(field.getIdentifier())
                                                         .identifierName(field.getIdentifier().getDisplayName())
                                                         .build())
                                              .collect(Collectors.toList());
    return QLCEViewFieldIdentifierData.builder()
        .identifier(ViewFieldIdentifier.CUSTOM)
        .identifierName(ViewFieldIdentifier.CUSTOM.getDisplayName())
        .values(ceViewFieldList)
        .build();
  }

  private static Optional<QLCEViewFilterWrapper> getViewMetadataFilter(List<QLCEViewFilterWrapper> filters) {
    return filters.stream().filter(f -> f.getViewMetadataFilter().getViewId() != null).findFirst();
  }
}
