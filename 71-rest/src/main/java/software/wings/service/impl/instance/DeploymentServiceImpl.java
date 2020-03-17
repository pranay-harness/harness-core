package software.wings.service.impl.instance;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.GE;
import static io.harness.beans.SearchFilter.Operator.LT;
import static io.harness.beans.SortOrder.Builder.aSortOrder;
import static io.harness.beans.SortOrder.OrderType.ASC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeValidate;
import static java.util.Arrays.asList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.api.ContainerDeploymentInfoWithNames;
import software.wings.api.DeploymentSummary;
import software.wings.api.DeploymentSummary.DeploymentSummaryKeys;
import software.wings.api.K8sDeploymentInfo;
import software.wings.beans.infrastructure.instance.key.deployment.AwsAmiDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.AwsCodeDeployDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.AwsLambdaDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.ContainerDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.K8sDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.PcfDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.SpotinstAmiDeploymentKey;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.instance.DeploymentService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.Valid;

@Singleton
@Slf4j
public class DeploymentServiceImpl implements DeploymentService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public DeploymentSummary save(@Valid DeploymentSummary deploymentSummary) {
    Query<DeploymentSummary> query = wingsPersistence.createAuthorizedQuery(DeploymentSummary.class);
    query.filter("infraMappingId", deploymentSummary.getInfraMappingId());
    DeploymentKey deploymentKey = addDeploymentKeyFilterToQuery(query, deploymentSummary);
    query.order(Sort.descending(DeploymentSummary.CREATED_AT_KEY));

    if (query.get() == null) {
      synchronized (deploymentKey) {
        String key = wingsPersistence.save(deploymentSummary);
        return wingsPersistence.getWithAppId(DeploymentSummary.class, deploymentSummary.getAppId(), key);
      }
    }

    return deploymentSummary;
  }

  @Override
  public Optional<DeploymentSummary> get(@Valid DeploymentSummary deploymentSummary) {
    Query<DeploymentSummary> query = wingsPersistence.createAuthorizedQuery(DeploymentSummary.class);
    // If later someone needs to extend deploymentKey to add more attributes to key, this method can be modified
    // to check keyClass and perform required chack
    query.filter("infraMappingId", deploymentSummary.getInfraMappingId());
    addDeploymentKeyFilterToQuery(query, deploymentSummary);
    query.order(Sort.descending(DeploymentSummary.CREATED_AT_KEY));
    DeploymentSummary summary = query.get();
    if (summary == null) {
      return Optional.empty();
    }

    return Optional.of(summary);
  }

  @Override
  public Optional<DeploymentSummary> getWithAccountId(DeploymentSummary deploymentSummary) {
    Query<DeploymentSummary> query = wingsPersistence.createQuery(DeploymentSummary.class, excludeValidate);
    query.filter(DeploymentSummaryKeys.accountId, deploymentSummary.getAccountId());
    addDeploymentInfoFilterToQuery(query, deploymentSummary);
    query.order(Sort.descending(DeploymentSummary.CREATED_AT_KEY));
    DeploymentSummary summary = query.get();
    return Optional.ofNullable(summary);
  }

  @Override
  public List<DeploymentSummary> getDeploymentSummary(
      String accountId, String offset, Instant startTime, Instant endTime) {
    PageRequest<DeploymentSummary> pageRequest =
        aPageRequest().withOffset(offset).withLimit(String.valueOf(PageRequest.DEFAULT_PAGE_SIZE)).build();
    pageRequest.addFilter(DeploymentSummaryKeys.accountId, EQ, accountId);
    pageRequest.addFilter(DeploymentSummaryKeys.CREATED_AT, GE, startTime.toEpochMilli());
    pageRequest.addFilter(DeploymentSummaryKeys.CREATED_AT, LT, endTime.toEpochMilli());
    pageRequest.setOrders(asList(aSortOrder().withField(DeploymentSummaryKeys.accountId, ASC).build(),
        aSortOrder().withField(DeploymentSummaryKeys.CREATED_AT, ASC).build()));
    return wingsPersistence.query(DeploymentSummary.class, pageRequest).getResponse();
  }

  private void addDeploymentInfoFilterToQuery(Query<DeploymentSummary> query, DeploymentSummary deploymentSummary) {
    if (deploymentSummary.getContainerDeploymentKey() != null) {
      ContainerDeploymentInfoWithNames deploymentInfo =
          (ContainerDeploymentInfoWithNames) deploymentSummary.getDeploymentInfo();
      if (deploymentInfo.getContainerSvcName() != null) {
        query.filter(
            DeploymentSummaryKeys.CLUSTER_NAME_CONTAINER_DEPLOYMENT_INFO_WITH_NAMES, deploymentInfo.getClusterName());
        query.filter(DeploymentSummaryKeys.CONTAINER_SVC_NAME_CONTAINER_DEPLOYMENT_INFO_WITH_NAMES,
            deploymentInfo.getContainerSvcName());
      }
    } else if (deploymentSummary.getK8sDeploymentKey() != null) {
      K8sDeploymentInfo deploymentInfo = (K8sDeploymentInfo) deploymentSummary.getDeploymentInfo();
      query.filter(DeploymentSummaryKeys.RELEASE_NAME_K8S_DEPLOYMENT_INFO, deploymentInfo.getReleaseName());
    }
  }

  private DeploymentKey addDeploymentKeyFilterToQuery(
      Query<DeploymentSummary> query, DeploymentSummary deploymentSummary) {
    if (deploymentSummary.getPcfDeploymentKey() != null) {
      PcfDeploymentKey pcfDeploymentKey = deploymentSummary.getPcfDeploymentKey();
      query.filter("pcfDeploymentKey.applicationName", pcfDeploymentKey.getApplicationName());
      return pcfDeploymentKey;
    } else if (deploymentSummary.getK8sDeploymentKey() != null) {
      K8sDeploymentKey k8sDeploymentKey = deploymentSummary.getK8sDeploymentKey();
      query.filter("k8sDeploymentKey.releaseName", k8sDeploymentKey.getReleaseName());
      query.filter("k8sDeploymentKey.releaseNumber", k8sDeploymentKey.getReleaseNumber());
      return k8sDeploymentKey;
    } else if (deploymentSummary.getContainerDeploymentKey() != null) {
      return AddDeploymentKeyFilterForContainer(query, deploymentSummary);
    } else if (deploymentSummary.getAwsAmiDeploymentKey() != null) {
      AwsAmiDeploymentKey awsAmiDeploymentKey = deploymentSummary.getAwsAmiDeploymentKey();
      query.filter("awsAmiDeploymentKey.autoScalingGroupName", awsAmiDeploymentKey.getAutoScalingGroupName());
      return awsAmiDeploymentKey;
    } else if (deploymentSummary.getAwsCodeDeployDeploymentKey() != null) {
      AwsCodeDeployDeploymentKey awsCodeDeployDeploymentKey = deploymentSummary.getAwsCodeDeployDeploymentKey();
      query.filter("awsCodeDeployDeploymentKey.key", awsCodeDeployDeploymentKey.getKey());
      return awsCodeDeployDeploymentKey;
    } else if (deploymentSummary.getSpotinstAmiDeploymentKey() != null) {
      SpotinstAmiDeploymentKey spotinstAmiDeploymentKey = deploymentSummary.getSpotinstAmiDeploymentKey();
      query.filter("spotinstAmiDeploymentKey.elastigroupId", spotinstAmiDeploymentKey.getElastigroupId());
      return spotinstAmiDeploymentKey;
    } else if (deploymentSummary.getAwsLambdaDeploymentKey() != null) {
      final AwsLambdaDeploymentKey awsLambdaDeploymentKey = deploymentSummary.getAwsLambdaDeploymentKey();
      query.filter("awsLambdaDeploymentKey.functionName", awsLambdaDeploymentKey.getFunctionName());
      query.filter("awsLambdaDeploymentKey.version", awsLambdaDeploymentKey.getVersion());
      return awsLambdaDeploymentKey;
    } else {
      String msg = "Either AMI, CodeDeploy, container or pcf deployment key needs to be set";
      logger.error(msg);
      throw new WingsException(msg);
    }
  }

  private DeploymentKey AddDeploymentKeyFilterForContainer(
      Query<DeploymentSummary> query, DeploymentSummary deploymentSummary) {
    ContainerDeploymentKey containerDeploymentKey = deploymentSummary.getContainerDeploymentKey();
    if (isNotEmpty(containerDeploymentKey.getContainerServiceName())) {
      query.filter("containerDeploymentKey.containerServiceName", containerDeploymentKey.getContainerServiceName());
    } else if (isNotEmpty(containerDeploymentKey.getLabels())) {
      query.field("containerDeploymentKey.labels").hasAllOf(containerDeploymentKey.getLabels());
      if (isNotEmpty(containerDeploymentKey.getNewVersion())) {
        query.filter("containerDeploymentKey.newVersion", containerDeploymentKey.getNewVersion());
      }
    }
    return containerDeploymentKey;
  }

  @Override
  public DeploymentSummary get(String id) {
    return wingsPersistence.get(DeploymentSummary.class, id);
  }

  @Override
  public void pruneByApplication(String appId) {
    Query<DeploymentSummary> query = wingsPersistence.createAuthorizedQuery(DeploymentSummary.class);
    query.filter("appId", appId);
    wingsPersistence.delete(query);
  }

  @Override
  public boolean delete(Set<String> idSet) {
    Query<DeploymentSummary> query = wingsPersistence.createAuthorizedQuery(DeploymentSummary.class);
    query.field("_id").in(idSet);
    return wingsPersistence.delete(query);
  }

  @Override
  public PageResponse<DeploymentSummary> list(PageRequest<DeploymentSummary> pageRequest) {
    return wingsPersistence.query(DeploymentSummary.class, pageRequest);
  }
}
