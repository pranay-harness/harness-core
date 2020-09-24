package io.harness.ccm.views.graphql;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.ccm.views.utils.ViewFieldUtils;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLNoOpQueryParameters;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.List;
import java.util.stream.Collectors;

public class ViewFieldsDataFetcher extends AbstractObjectDataFetcher<QLCEViewFieldsData, QLNoOpQueryParameters> {
  @Inject private ViewCustomFieldService viewCustomFieldService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLCEViewFieldsData fetch(QLNoOpQueryParameters parameters, String accountId) {
    List<ViewField> customFields = viewCustomFieldService.getCustomFields(accountId);
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
    return QLCEViewFieldIdentifierData.builder().identifier(viewFieldIdentifier).values(ceViewFieldList).build();
  }

  private QLCEViewFieldIdentifierData getViewCustomField(List<ViewField> customFields) {
    List<QLCEViewField> ceViewFieldList = customFields.stream()
                                              .map(field
                                                  -> QLCEViewField.builder()
                                                         .fieldId(field.getFieldId())
                                                         .fieldName(field.getFieldName())
                                                         .identifier(field.getIdentifier())
                                                         .build())
                                              .collect(Collectors.toList());
    return QLCEViewFieldIdentifierData.builder().identifier(ViewFieldIdentifier.CUSTOM).values(ceViewFieldList).build();
  }
}
