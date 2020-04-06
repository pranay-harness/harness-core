package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.Scope;
import io.harness.beans.SweepingOutputInstance.SweepingOutputInstanceBuilder;
import io.harness.beans.SweepingOutputInstance.SweepingOutputKeys;
import io.harness.deployment.InstanceDetails;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.CriteriaContainerImpl;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.api.InstanceElement;
import software.wings.api.instancedetails.InstanceInfoVariables;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiryController;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.StateExecutionInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@Slf4j
public class SweepingOutputServiceImpl implements SweepingOutputService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public SweepingOutputInstance save(SweepingOutputInstance sweepingOutputInstance) {
    try {
      wingsPersistence.save(sweepingOutputInstance);
      return sweepingOutputInstance;
    } catch (DuplicateKeyException exception) {
      throw new InvalidRequestException(
          format("Output with name %s, already saved in the context", sweepingOutputInstance.getName()), exception);
    }
  }

  @Override
  public void ensure(SweepingOutputInstance sweepingOutputInstance) {
    wingsPersistence.saveIgnoringDuplicateKeys(Collections.singletonList(sweepingOutputInstance));
  }

  @Override
  public void cleanForStateExecutionInstance(StateExecutionInstance stateExecutionInstance) {
    SweepingOutputInquiry sweepingOutputInquiry =
        SweepingOutputInquiryController.obtainFromStateExecutionInstanceWithoutName(stateExecutionInstance);
    final Query<SweepingOutputInstance> query =
        wingsPersistence.createQuery(SweepingOutputInstance.class)
            .filter(SweepingOutputKeys.appId, sweepingOutputInquiry.getAppId())
            .filter(SweepingOutputKeys.stateExecutionId, stateExecutionInstance.getUuid());
    wingsPersistence.delete(query);
  }

  @Override
  public void copyOutputsForAnotherWorkflowExecution(
      String appId, String fromWorkflowExecutionId, String toWorkflowExecutionId) {
    if (fromWorkflowExecutionId.equals(toWorkflowExecutionId)) {
      return;
    }
    final Query<SweepingOutputInstance> query =
        wingsPersistence.createQuery(SweepingOutputInstance.class)
            .filter(SweepingOutputKeys.appId, appId)
            .filter(SweepingOutputKeys.workflowExecutionIds, fromWorkflowExecutionId);

    UpdateOperations<SweepingOutputInstance> ops =
        wingsPersistence.createUpdateOperations(SweepingOutputInstance.class);
    ops.addToSet(SweepingOutputKeys.workflowExecutionIds, toWorkflowExecutionId);
    wingsPersistence.update(query, ops);
  }

  @Override
  public Query<SweepingOutputInstance> prepareApprovalStateOutputsQuery(
      String appId, String fromPipelineExecutionId, String fromStateExecutionId) {
    return wingsPersistence.createQuery(SweepingOutputInstance.class)
        .filter(SweepingOutputKeys.appId, appId)
        .filter(SweepingOutputKeys.pipelineExecutionId, fromPipelineExecutionId)
        .filter(SweepingOutputKeys.stateExecutionId, fromStateExecutionId);
  }

  @Override
  public Query<SweepingOutputInstance> prepareEnvStateOutputsQuery(
      String appId, String fromPipelineExecutionId, String fromWorkflowExecutionId) {
    return wingsPersistence.createQuery(SweepingOutputInstance.class)
        .filter(SweepingOutputKeys.appId, appId)
        .filter(SweepingOutputKeys.pipelineExecutionId, fromPipelineExecutionId)
        .filter(SweepingOutputKeys.workflowExecutionIds, fromWorkflowExecutionId);
  }

  @Override
  public <T extends SweepingOutput> T findSweepingOutput(SweepingOutputInquiry inquiry) {
    SweepingOutputInstance sweepingOutputInstance = find(inquiry);
    if (sweepingOutputInstance == null) {
      return null;
    }
    return (T) sweepingOutputInstance.getValue();
  }

  @Override
  public <T extends SweepingOutput> List<T> findSweepingOutputsWithNamePrefix(
      SweepingOutputInquiry inquiry, Scope scope) {
    List<SweepingOutputInstance> sweepingOutputInstances = findManyWithNamePrefix(inquiry, scope);
    if (sweepingOutputInstances == null) {
      return null;
    }
    return (List<T>) sweepingOutputInstances.stream()
        .map(SweepingOutputInstance::getValue)
        .collect(Collectors.toList());
  }

  @Override
  public List<InstanceDetails> fetchInstanceDetailsFromSweepingOutput(
      SweepingOutputInquiry inquiry, boolean newInstancesOnly) {
    List<InstanceDetails> instanceDetails = new ArrayList<>();
    SweepingOutput sweepingOutput = findSweepingOutput(inquiry);
    if (sweepingOutput instanceof InstanceInfoVariables) {
      InstanceInfoVariables instanceInfoVariables = (InstanceInfoVariables) sweepingOutput;
      if (isNotEmpty(instanceInfoVariables.getInstanceElements())) {
        instanceDetails.addAll(instanceInfoVariables.getInstanceDetails());
        if (newInstancesOnly) {
          instanceDetails = instanceDetails.stream()
                                .filter(instanceDetail -> instanceDetail.isNewInstance())
                                .collect(Collectors.toList());
        }
      }
    }

    return instanceDetails;
  }

  @Override
  public List<InstanceElement> fetchInstanceElementsFromSweepingOutput(
      SweepingOutputInquiry inquiry, boolean newInstancesOnly) {
    List<InstanceElement> elements = new ArrayList<>();
    SweepingOutput sweepingOutput = findSweepingOutput(inquiry);
    if (sweepingOutput instanceof InstanceInfoVariables) {
      InstanceInfoVariables instanceInfoVariables = (InstanceInfoVariables) sweepingOutput;
      if (isNotEmpty(instanceInfoVariables.getInstanceElements())) {
        elements.addAll(instanceInfoVariables.getInstanceElements());
        if (newInstancesOnly) {
          elements =
              elements.stream().filter(instanceElement -> instanceElement.isNewInstance()).collect(Collectors.toList());
        }
      }
    }

    return elements;
  }

  @Override
  public SweepingOutputInstance find(SweepingOutputInquiry sweepingOutputInquiry) {
    final Query<SweepingOutputInstance> query = wingsPersistence.createQuery(SweepingOutputInstance.class)
                                                    .filter(SweepingOutputKeys.appId, sweepingOutputInquiry.getAppId())
                                                    .filter(SweepingOutputKeys.name, sweepingOutputInquiry.getName());

    addFilters(sweepingOutputInquiry, query);
    return query.get();
  }

  @Override
  public List<SweepingOutputInstance> findManyWithNamePrefix(SweepingOutputInquiry sweepingOutputInquiry, Scope scope) {
    final Query<SweepingOutputInstance> query = wingsPersistence.createQuery(SweepingOutputInstance.class)
                                                    .filter(SweepingOutputKeys.appId, sweepingOutputInquiry.getAppId())
                                                    .field(SweepingOutputKeys.name)
                                                    .startsWith(sweepingOutputInquiry.getName())
                                                    .order(Sort.ascending(SweepingOutputKeys.createdAt));

    addFiltersWithScope(sweepingOutputInquiry, query, scope);
    return query.asList();
  }

  private void addFiltersWithScope(
      SweepingOutputInquiry sweepingOutputInquiry, Query<SweepingOutputInstance> query, Scope scope) {
    CriteriaContainerImpl criteria = null;
    switch (scope) {
      case PIPELINE:
        if (sweepingOutputInquiry.getPipelineExecutionId() != null) {
          criteria = query.criteria(SweepingOutputKeys.pipelineExecutionId)
                         .equal(sweepingOutputInquiry.getPipelineExecutionId());
        }
        break;
      case WORKFLOW:
        criteria = query.criteria(SweepingOutputKeys.workflowExecutionIds)
                       .equal(sweepingOutputInquiry.getWorkflowExecutionId());
        break;
      case PHASE:
        criteria =
            query.criteria(SweepingOutputKeys.phaseExecutionId).equal(sweepingOutputInquiry.getPhaseExecutionId());
        break;
      case STATE:
        criteria =
            query.criteria(SweepingOutputKeys.stateExecutionId).equal(sweepingOutputInquiry.getStateExecutionId());
        break;
      default:
        logger.error("Invalid scope", scope);
    }
    if (criteria != null) {
      query.and(criteria);
    }
  }

  private void addFilters(SweepingOutputInquiry sweepingOutputInquiry, Query<SweepingOutputInstance> query) {
    final CriteriaContainerImpl workflowCriteria =
        query.criteria(SweepingOutputKeys.workflowExecutionIds).equal(sweepingOutputInquiry.getWorkflowExecutionId());
    final CriteriaContainerImpl phaseCriteria =
        query.criteria(SweepingOutputKeys.phaseExecutionId).equal(sweepingOutputInquiry.getPhaseExecutionId());
    final CriteriaContainerImpl stateCriteria =
        query.criteria(SweepingOutputKeys.stateExecutionId).equal(sweepingOutputInquiry.getStateExecutionId());

    if (sweepingOutputInquiry.getPipelineExecutionId() != null) {
      final CriteriaContainerImpl pipelineCriteria =
          query.criteria(SweepingOutputKeys.pipelineExecutionId).equal(sweepingOutputInquiry.getPipelineExecutionId());
      query.or(pipelineCriteria, workflowCriteria, phaseCriteria, stateCriteria);
    } else {
      query.or(workflowCriteria, phaseCriteria, stateCriteria);
    }
  }

  public static SweepingOutputInstanceBuilder prepareSweepingOutputBuilder(String appId, String pipelineExecutionId,
      String workflowExecutionId, String phaseExecutionId, String stateExecutionId,
      SweepingOutputInstance.Scope sweepingOutputScope) {
    // Default scope is pipeline

    if (pipelineExecutionId == null || Scope.PIPELINE != sweepingOutputScope) {
      pipelineExecutionId = "dummy-" + generateUuid();
    }
    if (workflowExecutionId == null
        || (Scope.PIPELINE != sweepingOutputScope && Scope.WORKFLOW != sweepingOutputScope)) {
      workflowExecutionId = "dummy-" + generateUuid();
    }
    if (phaseExecutionId == null || Scope.STATE == sweepingOutputScope) {
      phaseExecutionId = "dummy-" + generateUuid();
    }
    if (stateExecutionId == null) {
      stateExecutionId = "dummy-" + generateUuid();
    }

    return SweepingOutputInstance.builder()
        .uuid(generateUuid())
        .appId(appId)
        .pipelineExecutionId(pipelineExecutionId)
        .workflowExecutionId(workflowExecutionId)
        .phaseExecutionId(phaseExecutionId)
        .stateExecutionId(stateExecutionId);
  }
}
