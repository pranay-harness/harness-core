package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.WingsException;

import software.wings.beans.Log;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCEEnabledFilter;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCloudProviderFilter;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCloudProviderTypeFilter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.SQLException;
import java.util.List;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.ValidationException;

/**
 * @author rktummala on 07/12/19
 */
@Singleton
@TargetModule(Module._380_CG_GRAPHQL)
public class CloudProviderQueryHelper {
  @Inject protected DataFetcherUtils utils;

  public void setQuery(List<QLCloudProviderFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<SettingAttribute>> field;

      if (filter.getCloudProvider() != null) {
        field = query.field("_id");
        QLIdFilter cloudProviderFilter = filter.getCloudProvider();
        utils.setIdFilter(field, cloudProviderFilter);
      }

      if (filter.getCloudProviderType() != null) {
        field = query.field("value.type");
        QLCloudProviderTypeFilter cloudProviderTypeFilter = filter.getCloudProviderType();
        utils.setEnumFilter(field, cloudProviderTypeFilter);
      }

      if (filter.getIsCEEnabled() != null) {
        QLCEEnabledFilter ceEnabledFilter = filter.getIsCEEnabled();
        QLEnumOperator operator = (QLEnumOperator) ceEnabledFilter.getOperator();
        if (operator == null) {
          throw new WingsException("Enum Operator cannot be null");
        }
        if (operator != QLEnumOperator.EQUALS) {
          throw new WingsException("Unknown Enum operator " + operator);
        }
        if (isEmpty(ceEnabledFilter.getValues())) {
          throw new WingsException("Value cannot be empty");
        }
        Boolean[] booleanFilterValues = ceEnabledFilter.getValues();
        if (booleanFilterValues.length > 1) {
          throw new WingsException("Only one value needs to be inputted for operator EQUALS");
        }
        if (booleanFilterValues[0] != true && booleanFilterValues[0] != false) {
          throw new WingsException("Value needs to be true or false");
        }
        query.disableValidation();
        query.filter(SettingAttributeKeys.isCEEnabled, booleanFilterValues[0]);
      }

      if (filter.getCreatedAt() != null) {
        field = query.field("createdAt");
        QLTimeFilter createdAtFilter = filter.getCreatedAt();
        utils.setTimeFilter(field, createdAtFilter);
      }
    });
  }
}
