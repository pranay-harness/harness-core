package software.wings.common;

import io.harness.beans.SweepingOutput;
import io.harness.context.ContextElementType;

import software.wings.api.RancherClusterElement;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExpressionProcessor;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Transient;

@NoArgsConstructor
public class RancherK8sClusterProcessor implements ExpressionProcessor {
  @Inject @Transient private SweepingOutputService sweepingOutputService;

  private ExecutionContext context;

  public static final String DEFAULT_EXPRESSION = "${rancherClusters}";

  private static final String EXPRESSION_START_PATTERN = "rancherClusters()";
  private static final String EXPRESSION_EQUAL_PATTERN = "rancherClusters";
  private static final String INSTANCE_EXPR_PROCESSOR = "rancherK8sClusterProcessor";

  public RancherK8sClusterProcessor(ExecutionContext context) {
    this.context = context;
  }

  @Override
  public String getPrefixObjectName() {
    return INSTANCE_EXPR_PROCESSOR;
  }

  @Override
  public List<String> getExpressionStartPatterns() {
    return Collections.singletonList(EXPRESSION_START_PATTERN);
  }

  @Override
  public List<String> getExpressionEqualPatterns() {
    return Collections.singletonList(EXPRESSION_EQUAL_PATTERN);
  }

  @Override
  public ContextElementType getContextElementType() {
    return ContextElementType.RANCHER_K8S_CLUSTER_CRITERIA;
  }

  public RancherK8sClusterProcessor getRancherClusters() {
    return this;
  }

  public List<RancherClusterElement> list() {
    RancherClusterElementList clusterElementList =
        sweepingOutputService.findSweepingOutput(context.prepareSweepingOutputInquiryBuilder()
                                                     .name(RancherClusterElementList.getSweepingOutputID(context))
                                                     .build());

    return Objects.isNull(clusterElementList) ? null : clusterElementList.getRancherClusterElements();
  }

  @AllArgsConstructor
  @JsonTypeName("rancherClusterElementList")
  public static class RancherClusterElementList implements SweepingOutput {
    @Getter private List<RancherClusterElement> rancherClusterElements;

    public static final String SWEEPING_OUTPUT_TYPE = "rancherClusterElementList";
    public static final String SWEEPING_OUTPUT_NAME = "rancherClusterElementListData";

    @Override
    public String getType() {
      return SWEEPING_OUTPUT_TYPE;
    }

    public static String getSweepingOutputID(ExecutionContext context) {
      return SWEEPING_OUTPUT_NAME + context.getWorkflowExecutionId().trim();
    }
  }
}