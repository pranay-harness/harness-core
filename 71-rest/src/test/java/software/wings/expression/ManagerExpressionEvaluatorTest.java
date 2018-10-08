/**
 *
 */

package software.wings.expression;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.api.HostElement;
import software.wings.beans.SweepingOutput;
import software.wings.beans.infrastructure.Host;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.utils.KryoUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * The Class ManagerExpressionEvaluatorTest.
 *
 * @author Rishi
 */
public class ManagerExpressionEvaluatorTest extends WingsBaseTest {
  @Inject private ManagerExpressionEvaluator expressionEvaluator;
  @Inject private SweepingOutputService sweepingOutputService;

  @Test
  public void shouldSubstituteHostUrl() {
    Host host = new Host();
    host.setHostName("app123.application.com");
    Map<String, Object> context = new HashMap<>();
    context.put("host", host);

    String retValue = expressionEvaluator.substitute("http://${host.hostName}:8080/health/status", context);
    assertThat(retValue).isEqualTo("http://app123.application.com:8080/health/status");
  }

  @Test
  public void shouldSubstitutePartially() {
    Host host = new Host();
    host.setHostName("${HOST}.$DOMAIN.${COM}");
    Map<String, Object> context = new HashMap<>();
    context.put("host", host);
    String retValue = expressionEvaluator.substitute("http://${host.hostName}:${PORT}/health/status", context);
    assertThat(retValue).isEqualTo("http://${HOST}.$DOMAIN.${COM}:${PORT}/health/status");

    retValue = expressionEvaluator.substitute("http://${host.hostName}:${PORT}/health/status", context, "bar");
    assertThat(retValue).isEqualTo("http://${HOST}.$DOMAIN.${COM}:${PORT}/health/status");
  }

  @Test
  public void shouldEvaluateEc2Instance() {
    Instance ec2 = new Instance();
    ec2.setPrivateDnsName("ip-172-31-24-237.ec2.internal");
    ec2.setInstanceId("1qazxsw2");
    Tag tag1 = new Tag();
    tag1.setKey("name");
    tag1.setValue("foo");
    Tag tag2 = new Tag();
    tag2.setKey("type");
    tag2.setValue("bar");
    ec2.setTags(asList(tag1, tag2));

    HostElement host = HostElement.Builder.aHostElement().withEc2Instance(ec2).build();
    Map<String, Object> map = ImmutableMap.<String, Object>builder().put("host", host).build();

    assertThat(expressionEvaluator.substitute("${host.ec2Instance.privateDnsName.split('\\.')[0]}", map))
        .isEqualTo("ip-172-31-24-237");
    assertThat(expressionEvaluator.substitute("abc-${host.ec2Instance.instanceId}-def", map))
        .isEqualTo("abc-1qazxsw2-def");

    assertThat(expressionEvaluator.substitute("${aws.tags.find(host.ec2Instance.tags, 'name')}", map)).isEqualTo("foo");
    assertThat(expressionEvaluator.substitute("${aws.tags.find(host.ec2Instance.tags, 'type')}", map)).isEqualTo("bar");

    assertThat(expressionEvaluator.substitute("${aws.tags.find(host.ec2Instance.tags, 'missing')}", map)).isEqualTo("");
  }

  @Test
  public void shouldRenderSweepingOutputFunctor() {
    String appId = generateUuid();
    String pipelineExecutionId = generateUuid();
    String workflowExecutionId = generateUuid();
    String followingWorkflowExecutionId = generateUuid();

    SweepingOutput sweepingOutput =
        sweepingOutputService.save(SweepingOutput.builder()
                                       .name("jenkins")
                                       .appId(appId)
                                       .pipelineExecutionId(pipelineExecutionId)
                                       .workflowExecutionId(workflowExecutionId)
                                       .output(KryoUtils.asDeflatedBytes(ImmutableMap.of("foo", "bar")))
                                       .build());

    Map<String, Object> context = ImmutableMap.<String, Object>builder()
                                      .put("context",
                                          SweepingOutputFunctor.builder()
                                              .sweepingOutputService(sweepingOutputService)
                                              .appId(appId)
                                              .pipelineExecutionId(pipelineExecutionId)
                                              .workflowExecutionId(followingWorkflowExecutionId)
                                              .build())
                                      .build();

    assertThat(expressionEvaluator.substitute("${context.output(\"jenkins\").foo}", context)).isEqualTo("bar");
    assertThat(expressionEvaluator.substitute("${context.jenkins.foo}", context)).isEqualTo("bar");

    // Test that the first pass did not break the functionality
    assertThat(expressionEvaluator.substitute("${context.output(\"jenkins\").foo}", context)).isEqualTo("bar");
    assertThat(expressionEvaluator.substitute("${context.jenkins.foo}", context)).isEqualTo("bar");
  }

  @Test
  public void shouldRenderSweepingOutputValue() {
    String appId = generateUuid();
    String pipelineExecutionId = generateUuid();
    String workflowExecutionId = generateUuid();
    String followingWorkflowExecutionId = generateUuid();

    SweepingOutput sweepingOutput =
        sweepingOutputService.save(SweepingOutput.builder()
                                       .name("workflow")
                                       .appId(appId)
                                       .pipelineExecutionId(pipelineExecutionId)
                                       .workflowExecutionId(workflowExecutionId)
                                       .output(KryoUtils.asDeflatedBytes(ImmutableMap.of("foo", "bar")))
                                       .build());

    Map<String, Object> context = ImmutableMap.<String, Object>builder()
                                      .put("workflow",
                                          SweepingOutputValue.builder()
                                              .sweepingOutputService(sweepingOutputService)
                                              .name("workflow")
                                              .appId(appId)
                                              .pipelineExecutionId(pipelineExecutionId)
                                              .workflowExecutionId(followingWorkflowExecutionId)
                                              .build())
                                      .build();

    assertThat(expressionEvaluator.substitute("${workflow.foo}", context)).isEqualTo("bar");
  }
}
