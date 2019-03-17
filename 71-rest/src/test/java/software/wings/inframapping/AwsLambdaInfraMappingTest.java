package software.wings.inframapping;

import static org.junit.Assert.assertEquals;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;

import java.util.HashMap;
import java.util.Map;

public class AwsLambdaInfraMappingTest extends WingsBaseTest {
  private AwsLambdaInfraStructureMapping awsLambdaInfraStructureMapping;

  @Before
  public void setUp() throws Exception {
    awsLambdaInfraStructureMapping = new AwsLambdaInfraStructureMapping();
  }

  @Test
  @Category(UnitTests.class)
  public void testApplyProvisionerVariables() {
    Map<String, Object> outputVariablesMap = new HashMap<>();

    outputVariablesMap.put("region", "testRegion");
    outputVariablesMap.put("role", "testRole");
    outputVariablesMap.put("vpcId", "testVpc");

    awsLambdaInfraStructureMapping.applyProvisionerVariables(
        outputVariablesMap, NodeFilteringType.AWS_AUTOSCALING_GROUP);
    assertEquals("testRegion", awsLambdaInfraStructureMapping.getRegion());
    assertEquals("testRole", awsLambdaInfraStructureMapping.getRole());
    assertEquals("testVpc", awsLambdaInfraStructureMapping.getVpcId());
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testInvalidKey() {
    Map<String, Object> outputVariablesMap = new HashMap<>();

    outputVariablesMap.put("region1", "testRegion");
    awsLambdaInfraStructureMapping.applyProvisionerVariables(
        outputVariablesMap, NodeFilteringType.AWS_AUTOSCALING_GROUP);
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testMandatoryFields() {
    Map<String, Object> outputVariablesMap = new HashMap<>();
    awsLambdaInfraStructureMapping.applyProvisionerVariables(
        outputVariablesMap, NodeFilteringType.AWS_AUTOSCALING_GROUP);
  }
}
