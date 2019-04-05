package software.wings.graphql.schema.type.resolvers;

import static software.wings.graphql.utils.GraphQLConstants.APPLICATION_TYPE;
import static software.wings.graphql.utils.GraphQLConstants.ARTIFACT_TYPE;
import static software.wings.graphql.utils.GraphQLConstants.DEBUG_INFO_TYPE;
import static software.wings.graphql.utils.GraphQLConstants.WORKFLOW_EXECUTION_TYPE;
import static software.wings.graphql.utils.GraphQLConstants.WORKFLOW_TYPE;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import software.wings.graphql.schema.type.ApplicationInfo;
import software.wings.graphql.schema.type.ArtifactInfo;
import software.wings.graphql.schema.type.WorkflowExecutionInfo;
import software.wings.graphql.schema.type.WorkflowInfo;
import software.wings.graphql.utils.GraphQLConstants;

import java.util.Map;

@Singleton
public class TypeResolverHelper {
  /**
   * Later, we should have TEST to make sure a fieldName is only used once
   * otherwise it may be overridden.
   * @return
   */
  public Map<String, TypeResolver> getTypeResolverMap() {
    return ImmutableMap.of(
        GraphQLConstants.RESULT_TYPE, getResultTypeResolver(), DEBUG_INFO_TYPE, getDebugInfoTypeResolver());
  }

  private TypeResolver getResultTypeResolver() {
    return env -> {
      Object javaObject = env.getObject();
      GraphQLObjectType graphQLObjectType = null;
      if (javaObject instanceof WorkflowInfo) {
        graphQLObjectType = env.getSchema().getObjectType(WORKFLOW_TYPE);
      } else if (javaObject instanceof WorkflowExecutionInfo) {
        graphQLObjectType = env.getSchema().getObjectType(WORKFLOW_EXECUTION_TYPE);
      } else if (javaObject instanceof ApplicationInfo) {
        graphQLObjectType = env.getSchema().getObjectType(APPLICATION_TYPE);
      }
      return graphQLObjectType;
    };
  }

  private TypeResolver getDebugInfoTypeResolver() {
    return env -> {
      Object javaObject = env.getObject();
      GraphQLObjectType graphQLObjectType = null;
      if (javaObject instanceof WorkflowInfo) {
        graphQLObjectType = env.getSchema().getObjectType(WORKFLOW_TYPE);
      } else if (javaObject instanceof WorkflowExecutionInfo) {
        graphQLObjectType = env.getSchema().getObjectType(WORKFLOW_EXECUTION_TYPE);
      } else if (javaObject instanceof ArtifactInfo) {
        graphQLObjectType = env.getSchema().getObjectType(ARTIFACT_TYPE);
      } else if (javaObject instanceof ApplicationInfo) {
        graphQLObjectType = env.getSchema().getObjectType(APPLICATION_TYPE);
      }
      return graphQLObjectType;
    };
  }
}
