package software.wings.search.entities.application;

import com.google.inject.Inject;

import com.mongodb.DBObject;
import io.harness.persistence.HIterator;
import org.mongodb.morphia.query.Sort;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.AuditHeaderKeys;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentKeys;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.dl.WingsPersistence;
import software.wings.search.entities.related.audit.RelatedAuditView;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.framework.EntityInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ApplicationViewBuilder {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  private ApplicationView applicationView;
  private static final int DAYS_TO_RETAIN = 7;
  private static final int MAX_RELATED_ENTITIES_COUNT = 3;

  private void createBaseView(Application application) {
    applicationView = new ApplicationView(application.getUuid(), application.getName(), application.getDescription(),
        application.getAccountId(), application.getCreatedAt(), application.getLastUpdatedAt(), EntityType.APPLICATION,
        application.getCreatedBy(), application.getLastUpdatedBy());
  }

  private void setServices(Application application) {
    Set<EntityInfo> services = new HashSet<>();
    HIterator<Service> iterator = new HIterator<>(
        wingsPersistence.createQuery(Service.class).field(ServiceKeys.appId).equal(application.getUuid()).fetch());
    while (iterator.hasNext()) {
      final Service service = iterator.next();
      EntityInfo entityInfo = new EntityInfo(service.getUuid(), service.getName());
      services.add(entityInfo);
    }
    applicationView.setServices(services);
  }

  private void setWorkflows(Application application) {
    Set<EntityInfo> workflows = new HashSet<>();
    HIterator<Workflow> iterator = new HIterator<>(
        wingsPersistence.createQuery(Workflow.class).field(WorkflowKeys.appId).equal(application.getUuid()).fetch());
    while (iterator.hasNext()) {
      final Workflow workflow = iterator.next();
      EntityInfo entityInfo = new EntityInfo(workflow.getUuid(), workflow.getName());
      workflows.add(entityInfo);
    }
    applicationView.setWorkflows(workflows);
  }

  private void setEnvironments(Application application) {
    Set<EntityInfo> environments = new HashSet<>();
    HIterator<Environment> iterator = new HIterator<>(wingsPersistence.createQuery(Environment.class)
                                                          .field(EnvironmentKeys.appId)
                                                          .equal(application.getUuid())
                                                          .fetch());
    while (iterator.hasNext()) {
      final Environment environment = iterator.next();
      EntityInfo entityInfo = new EntityInfo(environment.getUuid(), environment.getName());
      environments.add(entityInfo);
    }
    applicationView.setEnvironments(environments);
  }

  private void setPipelines(Application application) {
    Set<EntityInfo> pipelines = new HashSet<>();
    HIterator<Pipeline> iterator = new HIterator<>(
        wingsPersistence.createQuery(Pipeline.class).field(PipelineKeys.appId).equal(application.getUuid()).fetch());
    while (iterator.hasNext()) {
      final Pipeline pipeline = iterator.next();
      EntityInfo entityInfo = new EntityInfo(pipeline.getUuid(), pipeline.getName());
      pipelines.add(entityInfo);
    }
    applicationView.setPipelines(pipelines);
  }

  public void setAuditsAndAuditTimestamps(Application application) {
    long startTimestamp = System.currentTimeMillis() - DAYS_TO_RETAIN * 86400 * 1000;
    List<RelatedAuditView> audits = new ArrayList<>();
    List<Long> auditTimestamps = new ArrayList<>();
    HIterator<AuditHeader> iterator = new HIterator<>(wingsPersistence.createQuery(AuditHeader.class)
                                                          .field("entityAuditRecords.entityId")
                                                          .equal(application.getUuid())
                                                          .field(ApplicationKeys.createdAt)
                                                          .greaterThanOrEq(startTimestamp)
                                                          .order(Sort.descending(AuditHeaderKeys.createdAt))
                                                          .fetch());
    while (iterator.hasNext()) {
      final AuditHeader auditHeader = iterator.next();
      if (auditHeader.getEntityAuditRecords() != null) {
        for (EntityAuditRecord entityAuditRecord : auditHeader.getEntityAuditRecords()) {
          if (entityAuditRecord.getAffectedResourceType().equals(EntityType.APPLICATION.name())
              && entityAuditRecord.getAppId().equals(application.getUuid())) {
            if (audits.size() < MAX_RELATED_ENTITIES_COUNT) {
              audits.add(relatedAuditViewBuilder.getAuditRelatedEntityView(auditHeader, entityAuditRecord));
            }
            auditTimestamps.add(TimeUnit.MILLISECONDS.toSeconds(auditHeader.getCreatedAt()));
          }
        }
      }
    }
    Collections.reverse(audits);
    Collections.reverse(auditTimestamps);
    applicationView.setAudits(audits);
    applicationView.setAuditTimestamps(auditTimestamps);
  }

  public ApplicationView createApplicationView(Application application) {
    createBaseView(application);
    setWorkflows(application);
    setEnvironments(application);
    setPipelines(application);
    setServices(application);
    setAuditsAndAuditTimestamps(application);
    return applicationView;
  }

  public ApplicationView createApplicationView(Application application, DBObject changeDocument) {
    createBaseView(application);
    return applicationView;
  }
}
