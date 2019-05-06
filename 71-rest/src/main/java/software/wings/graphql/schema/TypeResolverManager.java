package software.wings.graphql.schema;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import graphql.schema.TypeResolver;
import software.wings.graphql.schema.type.QLExecutedBy;
import software.wings.graphql.schema.type.QLPipelineExecution;
import software.wings.graphql.schema.type.QLWorkflowExecution;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@Singleton
public class TypeResolverManager {
  // Uniface is a short for union or interface
  public static final class TypeResolverManagerUnifaces {
    public static final String Cause = "Cause";
    public static final String Execution = "Execution";
  }

  public static final class TypeResolverManagerTypes {
    public static final String ExecutedBy = "ExecutedBy";
    public static final String PipelineExecution = "PipelineExecution";
    public static final String WorkflowExecution = "WorkflowExecution";
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
