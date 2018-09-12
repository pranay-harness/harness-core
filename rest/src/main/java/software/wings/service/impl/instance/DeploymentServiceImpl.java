package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentSummary;
import software.wings.beans.infrastructure.instance.key.deployment.AwsAmiDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.AwsCodeDeployDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.ContainerDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.PcfDeploymentKey;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.instance.DeploymentService;

import java.util.Optional;
import java.util.Set;
import javax.validation.Valid;

@Singleton
public class DeploymentServiceImpl implements DeploymentService {
  private static final Logger logger = LoggerFactory.getLogger(DeploymentServiceImpl.class);
  private static final String CREATED_AT_KEY = "createdAt";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;

  @Override
  public DeploymentSummary save(@Valid DeploymentSummary deploymentSummary) {
    Query<DeploymentSummary> query = wingsPersistence.createAuthorizedQuery(DeploymentSummary.class);
    query.filter("infraMappingId", deploymentSummary.getInfraMappingId());
    DeploymentKey deploymentKey = addDeploymentKeyFilterToQuery(query, deploymentSummary);
    query.order(Sort.descending(CREATED_AT_KEY));

    if (query.get() == null) {
      synchronized (deploymentKey) {
        String key = wingsPersistence.save(deploymentSummary);
        return wingsPersistence.get(DeploymentSummary.class, deploymentSummary.getAppId(), key);
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
    query.order(Sort.descending(CREATED_AT_KEY));
    DeploymentSummary summary = query.get();
    if (summary == null) {
      return Optional.empty();
    }

    return Optional.of(summary);
  }

  private DeploymentKey addDeploymentKeyFilterToQuery(
      Query<DeploymentSummary> query, DeploymentSummary deploymentSummary) {
    if (deploymentSummary.getPcfDeploymentKey() != null) {
      PcfDeploymentKey pcfDeploymentKey = deploymentSummary.getPcfDeploymentKey();
      query.filter("pcfDeploymentKey.applicationName", pcfDeploymentKey.getApplicationName());
      return pcfDeploymentKey;
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
