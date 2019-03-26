package software.wings.graphql.typeresolver;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.graphql.datafetcher.SchemaFieldsEnum;
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
    return ImmutableMap.of(GraphQLConstants.RESPONSE_TYPE, getResponseTypeResolver());
  }

  private TypeResolver getResponseTypeResolver() {
    return env -> {
      Object javaObject = env.getObject();
      GraphQLObjectType graphQLObjectType = null;
      if (javaObject instanceof Workflow) {
        graphQLObjectType = env.getSchema().getObjectType(SchemaFieldsEnum.WORKFLOW.name());
      } else if (javaObject instanceof WorkflowExecution) {
        graphQLObjectType = env.getSchema().getObjectType(SchemaFieldsEnum.WORKFLOW_EXECUTION.name());
      }
      return graphQLObjectType;
    };
  }
}
