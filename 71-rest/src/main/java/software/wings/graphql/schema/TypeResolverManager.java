package software.wings.graphql.schema;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import graphql.schema.TypeResolver;
import software.wings.graphql.schema.type.QLDeploymentOutcome;
import software.wings.graphql.schema.type.QLExecutedBy;
import software.wings.graphql.schema.type.QLPipelineExecution;
import software.wings.graphql.schema.type.QLWorkflowExecution;
import software.wings.graphql.schema.type.cloudProvider.QLAwsCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLAzureCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLGcpCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLKubernetesClusterCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLPcfCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLPhysicalDataCenterCloudProvider;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@Singleton
public class TypeResolverManager {
  // Uniface is a short for union or interface
  public static final class TypeResolverManagerUnifaces {
    public static final String Cause = "Cause";
    public static final String Execution = "Execution";
    public static final String Outcome = "Outcome";
    public static final String CloudProvider = "CloudProvider";
  }

  public static final class TypeResolverManagerTypes {
    public static final String ExecutedBy = "ExecutedBy";
    public static final String PipelineExecution = "PipelineExecution";
    public static final String WorkflowExecution = "WorkflowExecution";
    public static final String DeploymentOutcome = "DeploymentOutcome";
    public static final String AwsCloudProvider = "AwsCloudProvider";
    public static final String PhysicalDataCenterCloudProvider = "PhysicalDataCenterCloudProvider";
    public static final String AzureCloudProvider = "AzureCloudProvider";
    public static final String GcpCloudProvider = "GcpCloudProvider";
    public static final String KubernetesCloudProvider = "KubernetesCloudProvider";
    public static final String PcfCloudProvider = "PcfCloudProvider";
  }

  /**
   * Later, we should have TEST to make sure a fieldName is only used once
   * otherwise it may be overridden.
   * @return
   */
  public Map<String, TypeResolver> getTypeResolverMap() {
    return ImmutableMap.<String, TypeResolver>builder()
        .put(TypeResolverManagerUnifaces.Execution,
            getResultTypeResolver(ImmutableMap.<Class, String>builder()
                                      .put(QLPipelineExecution.class, TypeResolverManagerTypes.PipelineExecution)
                                      .put(QLWorkflowExecution.class, TypeResolverManagerTypes.WorkflowExecution)
                                      .build()))
        .put(TypeResolverManagerUnifaces.Cause,
            getResultTypeResolver(ImmutableMap.<Class, String>builder()
                                      .put(QLExecutedBy.class, TypeResolverManagerTypes.ExecutedBy)
                                      .put(QLPipelineExecution.class, TypeResolverManagerTypes.PipelineExecution)
                                      .build()))
        .put(TypeResolverManagerUnifaces.Outcome,
            getResultTypeResolver(ImmutableMap.<Class, String>builder()
                                      .put(QLDeploymentOutcome.class, TypeResolverManagerTypes.DeploymentOutcome)
                                      .build()))
        .put(TypeResolverManagerUnifaces.CloudProvider,
            getResultTypeResolver(
                ImmutableMap.<Class, String>builder()
                    .put(QLAwsCloudProvider.class, TypeResolverManagerTypes.AwsCloudProvider)
                    .put(QLPhysicalDataCenterCloudProvider.class,
                        TypeResolverManagerTypes.PhysicalDataCenterCloudProvider)
                    .put(QLAzureCloudProvider.class, TypeResolverManagerTypes.AzureCloudProvider)
                    .put(QLGcpCloudProvider.class, TypeResolverManagerTypes.GcpCloudProvider)
                    .put(QLKubernetesClusterCloudProvider.class, TypeResolverManagerTypes.KubernetesCloudProvider)
                    .put(QLPcfCloudProvider.class, TypeResolverManagerTypes.PcfCloudProvider)
                    .build()))
        .build();
  }

  private TypeResolver getResultTypeResolver(Map<Class, String> types) {
    return env -> {
      Object javaObject = env.getObject();
      final Set<Entry<Class, String>> entries = types.entrySet();

      for (Entry<Class, String> entry : types.entrySet()) {
        if (entry.getKey().isAssignableFrom(javaObject.getClass())) {
          return env.getSchema().getObjectType(entry.getValue());
        }
      }

      return null;
    };
  }
}
