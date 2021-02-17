package software.wings.graphql.instrumentation;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import com.google.common.collect.Sets;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.parameters.InstrumentationValidationParameters;
import graphql.validation.ValidationError;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(Module._380_CG_GRAPHQL)
public class QueryDepthInstrumentation extends MaxQueryDepthInstrumentation {
  public static final int MAX_QUERY_DEPTH = 10;

  private static Set<String> INTROSPECTION_QUERY_TOKENS =
      Sets.newHashSet("", "query", "IntrospectionQuery", "schema", "queryType", "name", "mutationType",
          "subscriptionType", "types", "FullType", "directives", "description", "locations", "args", "InputValue",
          "fragment", "on", "Type", "kind", "fields", "includeDeprecated", "true", "type", "TypeRef", "isDeprecated",
          "deprecationReason", "inputFields", "interfaces", "enumValues", "possibleTypes", "defaultValue", "ofType");

  public QueryDepthInstrumentation() {
    super(MAX_QUERY_DEPTH);
  }

  @Override
  public InstrumentationContext<List<ValidationError>> beginValidation(InstrumentationValidationParameters parameters) {
    String query = parameters.getQuery();
    boolean isIntrospectionQuery = INTROSPECTION_QUERY_TOKENS.parallelStream().allMatch(query::contains);
    if (isIntrospectionQuery) {
      log.debug("This query is Introspection Query hence not applying max depth check");
      return new SimpleInstrumentationContext<>();
    } else {
      return super.beginValidation(parameters);
    }
  }
}
