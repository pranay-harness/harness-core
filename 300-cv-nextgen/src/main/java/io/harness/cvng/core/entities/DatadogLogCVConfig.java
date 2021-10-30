package io.harness.cvng.core.entities;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.io.Resources;
import io.harness.cvng.beans.DataSourceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

@JsonTypeName("DATADOG_LOG")
@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "DatadogLogCVConfigKeys")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DatadogLogCVConfig extends LogCVConfig {
    static final String DSL = readDSL("datadog-log-fetch-data.datacollection");

    List<String> indexes;
    String serviceInstanceIdentifier;

    @Override
    protected void validateParams() {
        checkNotNull(
                serviceInstanceIdentifier, generateErrorMessageFromParam(SplunkCVConfig.SplunkCVConfigKeys.serviceInstanceIdentifier));
    }

    @Override
    public DataSourceType getType() {
        return DataSourceType.DATADOG_LOG;
    }

    @Override
    public String getDataCollectionDsl() {
        return DSL;
    }

    @Override
    public String getHostCollectionDSL() {
        //  TODO slobodanpavic - should this be implemented???
        throw new RuntimeException("Not implemented");
    }

    private static String readDSL(String fileName) {
        try {
            return Resources.toString(DatadogLogCVConfig.class.getResource(fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    public static class DatadogLogCVConfigUpdatableEntity
            extends LogCVConfigUpdatableEntity<DatadogLogCVConfig, DatadogLogCVConfig> {
        @Override
        public void setUpdateOperations(
                UpdateOperations<DatadogLogCVConfig> updateOperations, DatadogLogCVConfig datadogLogCVConfig) {
            setCommonOperations(updateOperations, datadogLogCVConfig);
            updateOperations.set(DatadogLogCVConfigKeys.indexes, datadogLogCVConfig.getIndexes());
            updateOperations.set(
                    DatadogLogCVConfigKeys.serviceInstanceIdentifier, datadogLogCVConfig.getServiceInstanceIdentifier());
        }
    }
}
