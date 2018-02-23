package software.wings.service.impl.analysis;

import com.google.inject.Inject;

import software.wings.dl.WingsPersistence;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
public class CVServiceImpl implements CVService {
  @Inject protected WingsPersistence wingsPersistence;

  @Override
  public void saveCVExecutionMetaData(CVExecutionMetaData cvExecutionMetaData) {
    wingsPersistence.save(cvExecutionMetaData);
  }

  @Override
  public Map<Long, TreeMap<String, Map<String, Map<String, Map<String, List<CVExecutionMetaData>>>>>>
  getCVExecutionMetaData(String accountId, long beginEpochTs, long endEpochTs) throws ParseException {
    List<CVExecutionMetaData> cvExecutionMetaDatas = wingsPersistence.createQuery(CVExecutionMetaData.class)
                                                         .field("accountId")
                                                         .equal(accountId)
                                                         .field("workflowStartTs")
                                                         .greaterThanOrEq(beginEpochTs)
                                                         .field("workflowStartTs")
                                                         .lessThan(endEpochTs)
                                                         .order("-pipelineStartTs,-workflowStartTs")
                                                         .asList();

    Map<Long, TreeMap<String, Map<String, Map<String, Map<String, List<CVExecutionMetaData>>>>>> results =
        new HashMap<>();
    long startTimeTs = 0;
    for (CVExecutionMetaData executionMetaData : cvExecutionMetaDatas) {
      if (executionMetaData.getPipelineStartTs() != 0) {
        startTimeTs = executionMetaData.getPipelineStartTs();
      } else {
        startTimeTs = executionMetaData.getWorkflowStartTs();
      }
      startTimeTs = Instant.ofEpochMilli(startTimeTs).truncatedTo(ChronoUnit.DAYS).toEpochMilli();
      if (!results.containsKey(startTimeTs)) {
        results.put(startTimeTs, new TreeMap<>());
      }

      if (!results.get(startTimeTs).containsKey(executionMetaData.getArtifactName())) {
        results.get(startTimeTs).put(executionMetaData.getArtifactName(), new HashMap<>());
      }

      String envWorkflowName = executionMetaData.getEnvName() + "/" + executionMetaData.getWorkflowName();
      if (!results.get(startTimeTs).get(executionMetaData.getArtifactName()).containsKey(envWorkflowName)) {
        results.get(startTimeTs).get(executionMetaData.getArtifactName()).put(envWorkflowName, new HashMap<>());
      }

      if (!results.get(startTimeTs)
               .get(executionMetaData.getArtifactName())
               .get(envWorkflowName)
               .containsKey(executionMetaData.getWorkflowExecutionId())) {
        results.get(startTimeTs)
            .get(executionMetaData.getArtifactName())
            .get(envWorkflowName)
            .put(executionMetaData.getWorkflowExecutionId(), new HashMap<>());
      }

      String phaseName = executionMetaData.getPhaseName() == null ? "BASIC" : executionMetaData.getPhaseName();

      if (!results.get(startTimeTs)
               .get(executionMetaData.getArtifactName())
               .get(envWorkflowName)
               .get(executionMetaData.getWorkflowExecutionId())
               .containsKey(phaseName)) {
        results.get(startTimeTs)
            .get(executionMetaData.getArtifactName())
            .get(envWorkflowName)
            .get(executionMetaData.getWorkflowExecutionId())
            .put(phaseName, new ArrayList<>());
      }
      results.get(startTimeTs)
          .get(executionMetaData.getArtifactName())
          .get(envWorkflowName)
          .get(executionMetaData.getWorkflowExecutionId())
          .get(phaseName)
          .add(executionMetaData);
    }

    return results;
  }
}
