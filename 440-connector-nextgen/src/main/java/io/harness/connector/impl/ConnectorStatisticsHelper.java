package io.harness.connector.impl;

import static io.harness.NGCommonEntityConstants.MONGODB_ID;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.facet;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;

import io.harness.connector.apis.dto.stats.ConnectorStatistics;
import io.harness.connector.apis.dto.stats.ConnectorStatistics.ConnectorStatisticsKeys;
import io.harness.connector.apis.dto.stats.ConnectorStatusStats.ConnectorStatusStatsKeys;
import io.harness.connector.apis.dto.stats.ConnectorTypeStats.ConnectorTypeStatsKeys;
import io.harness.connector.entities.Connector.ConnectorKeys;
import io.harness.encryption.Scope;
import io.harness.repositories.ConnectorRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class ConnectorStatisticsHelper {
  ConnectorRepository connectorRepository;

  public ConnectorStatistics getStats(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, Scope scope) {
    Criteria criteria =
        createCriteriaObjectForConnectorScope(accountIdentifier, orgIdentifier, projectIdentifier, scope);
    MatchOperation matchStage = Aggregation.match(criteria);
    GroupOperation groupByType = group(ConnectorKeys.type).count().as(ConnectorTypeStatsKeys.count);
    ProjectionOperation projectType =
        project().and(MONGODB_ID).as(ConnectorTypeStatsKeys.type).andInclude(ConnectorTypeStatsKeys.count);
    GroupOperation groupByStatus = group(ConnectorKeys.connectionStatus).count().as(ConnectorStatusStatsKeys.count);
    ProjectionOperation projectStatus =
        project().and(MONGODB_ID).as(ConnectorStatusStatsKeys.status).andInclude(ConnectorStatusStatsKeys.count);
    FacetOperation facetOperation = facet(groupByType, projectType)
                                        .as(ConnectorStatisticsKeys.typeStats)
                                        .and(groupByStatus, projectStatus)
                                        .as(ConnectorStatisticsKeys.statusStats);
    Aggregation aggregation = Aggregation.newAggregation(matchStage, facetOperation);
    ConnectorStatistics connectorStatistics =
        connectorRepository.aggregate(aggregation, ConnectorStatistics.class).getUniqueMappedResult();
    return connectorStatistics;
  }

  private Criteria createCriteriaObjectForConnectorScope(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, Scope scope) {
    return Criteria.where(ConnectorKeys.accountIdentifier)
        .in(accountIdentifier)
        .and(ConnectorKeys.orgIdentifier)
        .in(orgIdentifier)
        .and(ConnectorKeys.projectIdentifier)
        .in(projectIdentifier)
        .and(ConnectorKeys.scope)
        .in(scope);
  }
}
