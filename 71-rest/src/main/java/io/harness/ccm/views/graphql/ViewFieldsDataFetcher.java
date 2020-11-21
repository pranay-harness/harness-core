package io.harness.ccm.views.graphql;

import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.ccm.views.utils.ViewFieldUtils;

import software.wings.graphql.datafetcher.AbstractFieldsDataFetcher;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ViewFieldsDataFetcher extends AbstractFieldsDataFetcher<QLCEViewFieldsData, QLCEViewFilterWrapper> {
  @Inject private ViewCustomFieldService viewCustomFieldService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLCEViewFieldsData fetch(String accountId, List<QLCEViewFilterWrapper> filters) {
    List<ViewField> customFields;
    Optional<QLCEViewFilterWrapper> viewMetadataFilter = getViewMetadataFilter(filters);
    if (viewMetadataFilter.isPresent()) {
      QLCEViewMetadataFilter metadataFilter = viewMetadataFilter.get().getViewMetadataFilter();
      final String viewId = metadataFilter.getViewId();
      customFields = viewCustomFieldService.getCustomFieldsPerView(viewId);
    } else {
      customFields = viewCustomFieldService.getCustomFields(accountId);
    }

    return QLCEViewFieldsData.builder()
        .fieldIdentifierData(ImmutableList.of(getViewField(ViewFieldUtils.getAwsFields(), ViewFieldIdentifier.AWS),
            getViewField(ViewFieldUtils.getGcpFields(), ViewFieldIdentifier.GCP),
            getViewField(ViewFieldUtils.getClusterFields(), ViewFieldIdentifier.CLUSTER),
            getViewField(ViewFieldUtils.getCommonFields(), ViewFieldIdentifier.COMMON),
            getViewCustomField(customFields)))
        .build();
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
