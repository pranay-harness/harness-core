/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.search;

import io.harness.beans.WorkflowType;
import io.harness.data.structure.UUIDGenerator;

import software.wings.beans.EntityType;
import software.wings.beans.WorkflowExecution;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.application.ApplicationView;
import software.wings.search.entities.deployment.DeploymentSearchEntity;
import software.wings.search.entities.deployment.DeploymentView;
import software.wings.search.entities.environment.EnvironmentSearchEntity;
import software.wings.search.entities.environment.EnvironmentView;
import software.wings.search.entities.pipeline.PipelineSearchEntity;
import software.wings.search.entities.pipeline.PipelineView;
import software.wings.search.entities.related.audit.RelatedAuditView;
import software.wings.search.entities.related.deployment.RelatedDeploymentView;
import software.wings.search.entities.service.ServiceSearchEntity;
import software.wings.search.entities.service.ServiceView;
import software.wings.search.entities.workflow.WorkflowSearchEntity;
import software.wings.search.entities.workflow.WorkflowView;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.TotalHits.Relation;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchResponse.Clusters;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.internal.InternalSearchResponse;

@Slf4j
public class SearchRequestHandlerTestUtils {
  private static float score = 0.2345f;
  private static ObjectMapper mapper = new ObjectMapper();

  private static WorkflowExecution workflowExecution = WorkflowExecution.builder()
                                                           .pipelineExecutionId(UUIDGenerator.generateUuid())
                                                           .workflowType(WorkflowType.PIPELINE)
                                                           .build();

  private static ApplicationView createApplicationView() {
    ApplicationView applicationView = new ApplicationView();
    applicationView.setId("appId" + System.currentTimeMillis());
    applicationView.setName("appName");
    applicationView.setType(EntityType.APPLICATION);
    applicationView.setCreatedAt(System.currentTimeMillis());

    List<Long> auditTimestamps = new ArrayList<>();
    auditTimestamps.add(System.currentTimeMillis());
    auditTimestamps.add(System.currentTimeMillis());
    applicationView.setAuditTimestamps(auditTimestamps);

    List<RelatedAuditView> audits = new ArrayList<>();
    audits.add(new RelatedAuditView());
    audits.add(new RelatedAuditView());
    applicationView.setAudits(audits);
    return applicationView;
  }

  private static ServiceView createServiceView() {
    ServiceView serviceView = new ServiceView();
    serviceView.setId("serviceId" + System.currentTimeMillis());
    serviceView.setName("serviceName");
    serviceView.setCreatedAt(System.currentTimeMillis());
    serviceView.setType(EntityType.SERVICE);

    serviceView.setAuditTimestamps(Arrays.asList(System.currentTimeMillis()));
    serviceView.setAudits(Arrays.asList(new RelatedAuditView()));
    serviceView.setDeploymentTimestamps(Arrays.asList(System.currentTimeMillis()));
    serviceView.setDeployments(Arrays.asList(new RelatedDeploymentView(workflowExecution)));
    return serviceView;
  }

  private static EnvironmentView createEnvironmentView() {
    EnvironmentView environmentView = new EnvironmentView();
    environmentView.setId("envId" + System.currentTimeMillis());
    environmentView.setName("envName");
    environmentView.setCreatedAt(System.currentTimeMillis());
    environmentView.setType(EntityType.ENVIRONMENT);

    environmentView.setAuditTimestamps(Arrays.asList(System.currentTimeMillis()));
    environmentView.setAudits(Arrays.asList(new RelatedAuditView()));

    environmentView.setDeploymentTimestamps(Arrays.asList(System.currentTimeMillis()));
    environmentView.setDeployments(Arrays.asList(new RelatedDeploymentView(workflowExecution)));
    return environmentView;
  }

  private static WorkflowView createWorkflowView() {
    WorkflowView workflowView = new WorkflowView();
    workflowView.setId("workflowId" + System.currentTimeMillis());
    workflowView.setName("workflowName");
    workflowView.setCreatedAt(System.currentTimeMillis());
    workflowView.setType(EntityType.WORKFLOW);
    workflowView.setAuditTimestamps(Arrays.asList(System.currentTimeMillis()));
    workflowView.setAudits(Arrays.asList(new RelatedAuditView()));

    workflowView.setDeploymentTimestamps(Arrays.asList(System.currentTimeMillis()));
    workflowView.setDeployments(Arrays.asList(new RelatedDeploymentView(workflowExecution)));
    return workflowView;
  }

  private static PipelineView createPipelineView() {
    PipelineView pipelineView = new PipelineView();
    pipelineView.setId("pipelineId" + System.currentTimeMillis());
    pipelineView.setName("pipelineName");
    pipelineView.setCreatedAt(System.currentTimeMillis());
    pipelineView.setType(EntityType.PIPELINE);
    pipelineView.setAuditTimestamps(Arrays.asList(System.currentTimeMillis()));
    pipelineView.setAudits(Arrays.asList(new RelatedAuditView()));

    pipelineView.setDeploymentTimestamps(Arrays.asList(System.currentTimeMillis()));
    pipelineView.setDeployments(Arrays.asList(new RelatedDeploymentView(workflowExecution)));
    return pipelineView;
  }

  private static DeploymentView createDeploymentView() {
    DeploymentView deploymentView = new DeploymentView();
    deploymentView.setId("deploymentId" + System.currentTimeMillis());
    deploymentView.setName("name");
    deploymentView.setCreatedAt(System.currentTimeMillis());
    deploymentView.setType(EntityType.DEPLOYMENT);
    deploymentView.setWorkflowInPipeline(false);
    return deploymentView;
  }

  private static SearchHit[] getDeploymentSearchHits() {
    try {
      DeploymentView deploymentView = createDeploymentView();
      BytesReference source = new BytesArray(mapper.writeValueAsBytes(deploymentView));
      SearchHit searchHit = new SearchHit(1);
      searchHit.score(score);
      searchHit.sourceRef(source);

      DeploymentView deploymentView1 = createDeploymentView();
      BytesReference source1 = new BytesArray(mapper.writeValueAsBytes(deploymentView1));
      SearchHit searchHit1 = new SearchHit(2);
      searchHit1.score(score);
      searchHit1.sourceRef(source1);

      return new SearchHit[] {searchHit, searchHit1};
    } catch (IOException e) {
      log.error("Error", e);
      return new SearchHit[] {};
    }
  }

  private static SearchHit[] getApplicationSearchHits() {
    try {
      ApplicationView applicationView = createApplicationView();
      BytesReference source = new BytesArray(mapper.writeValueAsBytes(applicationView));
      SearchHit searchHit = new SearchHit(1);
      searchHit.score(score);
      searchHit.sourceRef(source);

      return new SearchHit[] {searchHit};
    } catch (IOException e) {
      log.error("Error", e);
      return new SearchHit[] {};
    }
  }

  private static SearchHit[] getServiceSearchHits() {
    try {
      ServiceView serviceView = createServiceView();
      BytesReference source = new BytesArray(mapper.writeValueAsBytes(serviceView));
      SearchHit searchHit = new SearchHit(1);
      searchHit.score(score);
      searchHit.sourceRef(source);

      return new SearchHit[] {searchHit};
    } catch (IOException e) {
      return new SearchHit[] {};
    }
  }

  private static SearchHit[] getEnvironmentSearchHits() {
    try {
      EnvironmentView environmentView = createEnvironmentView();
      BytesReference source = new BytesArray(mapper.writeValueAsBytes(environmentView));
      SearchHit searchHit = new SearchHit(1);
      searchHit.score(score);
      searchHit.sourceRef(source);

      return new SearchHit[] {searchHit};
    } catch (IOException e) {
      return new SearchHit[] {};
    }
  }

  private static SearchHit[] getWorkflowSearchHits() {
    try {
      WorkflowView workflowView = createWorkflowView();
      BytesReference source = new BytesArray(mapper.writeValueAsBytes(workflowView));
      SearchHit searchHit = new SearchHit(1);
      searchHit.score(score);
      searchHit.sourceRef(source);

      return new SearchHit[] {searchHit};
    } catch (IOException e) {
      return new SearchHit[] {};
    }
  }

  private static SearchHit[] getPipelineSearchHits() {
    try {
      PipelineView pipelineView = createPipelineView();
      BytesReference source = new BytesArray(mapper.writeValueAsBytes(pipelineView));
      SearchHit searchHit = new SearchHit(1);
      searchHit.score(score);
      searchHit.sourceRef(source);

      return new SearchHit[] {searchHit};
    } catch (IOException e) {
      return new SearchHit[] {};
    }
  }

  public static SearchResponse getSearchResponse(String type) {
    ShardSearchFailure[] shardFailures = new ShardSearchFailure[0];
    SearchHit[] rawSearchHits = null;
    switch (type) {
      case ApplicationSearchEntity.TYPE:
        rawSearchHits = getApplicationSearchHits();
        break;
      case ServiceSearchEntity.TYPE:
        rawSearchHits = getServiceSearchHits();
        break;
      case WorkflowSearchEntity.TYPE:
        rawSearchHits = getWorkflowSearchHits();
        break;
      case PipelineSearchEntity.TYPE:
        rawSearchHits = getPipelineSearchHits();
        break;
      case DeploymentSearchEntity.TYPE:
        rawSearchHits = getDeploymentSearchHits();
        break;
      case EnvironmentSearchEntity.TYPE:
        rawSearchHits = getEnvironmentSearchHits();
        break;
      default:
        rawSearchHits = SearchHits.EMPTY;
    }
    SearchHits searchHits =
        new SearchHits(rawSearchHits, new TotalHits(rawSearchHits.length, Relation.EQUAL_TO), score);
    InternalSearchResponse internalSearchResponse =
        new InternalSearchResponse(searchHits, null, null, null, false, false, 1);

    return new SearchResponse(internalSearchResponse, "scrollId", 1, 1, 0, 10000, shardFailures, new Clusters(1, 1, 0));
  }
}
