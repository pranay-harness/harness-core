package io.harness.ccm.views.graphql;

import io.harness.ccm.views.entities.ViewCustomFunction;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.service.ViewCustomFieldService;

import com.google.inject.Inject;
import java.util.List;

public class CustomFieldExpressionHelper {
  @Inject private ViewCustomFieldService viewCustomFieldService;

  public String getSQLFormula(String userDefinedExpression, List<ViewField> fields) {
    for (ViewField field : fields) {
      switch (field.getIdentifier()) {
        case GCP:
        case AWS:
        case COMMON:
        case CLUSTER:
          userDefinedExpression = userDefinedExpression.replaceAll(field.getFieldName(), field.getFieldId());
          break;
        case CUSTOM:
          userDefinedExpression = userDefinedExpression.replaceAll(
              field.getFieldName(), viewCustomFieldService.get(field.getFieldId()).getSqlFormula());
          break;
        case LABEL:
          userDefinedExpression =
              userDefinedExpression.replaceAll(field.getFieldName(), ViewsMetaDataFields.LABEL_VALUE.getAlias());
          break;
        default:
          break;
      }
    }
    return userDefinedExpression.replaceAll(
        ViewCustomFunction.ONE_OF.getName(), ViewCustomFunction.ONE_OF.getFormula());
  }
}
