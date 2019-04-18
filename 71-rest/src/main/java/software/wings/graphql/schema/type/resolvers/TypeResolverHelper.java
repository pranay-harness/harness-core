package software.wings.graphql.schema.type.resolvers;

import static software.wings.graphql.utils.GraphQLConstants.APPLICATION_TYPE;
import static software.wings.graphql.utils.GraphQLConstants.ARTIFACT_TYPE;
import static software.wings.graphql.utils.GraphQLConstants.BASE_INFO_TYPE;
import static software.wings.graphql.utils.GraphQLConstants.ENVIRONMENT_TYPE;
import static software.wings.graphql.utils.GraphQLConstants.WORKFLOW_EXECUTION_TYPE;
import static software.wings.graphql.utils.GraphQLConstants.WORKFLOW_TYPE;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.graphql.schema.type.QLArtifact;
import software.wings.graphql.schema.type.QLEnvironment;
import software.wings.graphql.schema.type.QLWorkflow;
import software.wings.graphql.schema.type.QLWorkflowExecution;
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
        GraphQLConstants.RESULT_TYPE, getResultTypeResolver(), BASE_INFO_TYPE, getBaseInfoTypeResolver());
  }

  private TypeResolver getResultTypeResolver() {
    return env -> {
      Object javaObject = env.getObject();
      GraphQLObjectType graphQLObjectType = null;
      if (javaObject instanceof QLWorkflow) {
        graphQLObjectType = env.getSchema().getObjectType(WORKFLOW_TYPE);
      } else if (javaObject instanceof QLWorkflowExecution) {
        graphQLObjectType = env.getSchema().getObjectType(WORKFLOW_EXECUTION_TYPE);
      } else if (javaObject instanceof QLApplication) {
        graphQLObjectType = env.getSchema().getObjectType(APPLICATION_TYPE);
      } else if (javaObject instanceof QLEnvironment) {
        graphQLObjectType = env.getSchema().getObjectType(ENVIRONMENT_TYPE);
      }
      return graphQLObjectType;
    };
  }

  private TypeResolver getBaseInfoTypeResolver() {
    return env -> {
      Object javaObject = env.getObject();
      GraphQLObjectType graphQLObjectType = null;
      if (javaObject instanceof QLWorkflow) {
        graphQLObjectType = env.getSchema().getObjectType(WORKFLOW_TYPE);
      } else if (javaObject instanceof QLWorkflowExecution) {
        graphQLObjectType = env.getSchema().getObjectType(WORKFLOW_EXECUTION_TYPE);
      } else if (javaObject instanceof QLArtifact) {
        graphQLObjectType = env.getSchema().getObjectType(ARTIFACT_TYPE);
      } else if (javaObject instanceof QLApplication) {
        graphQLObjectType = env.getSchema().getObjectType(APPLICATION_TYPE);
      } else if (javaObject instanceof QLEnvironment) {
        graphQLObjectType = env.getSchema().getObjectType(ENVIRONMENT_TYPE);
      }
      return graphQLObjectType;
    };
  }
}
