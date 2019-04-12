package io.harness.checks;

import static io.harness.checks.AnnotationParametersCheck.Expectation.Type.EXISTS;
import static io.harness.checks.AnnotationParametersCheck.Expectation.Type.EXISTS_REGEX_MATCH;
import static java.util.Arrays.asList;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import io.harness.checks.mixin.AnnotationMixin;

import java.util.List;
import java.util.Map;

public class AnnotationParametersCheck extends AbstractCheck {
  private static final String MSG_EXIST_KEY = "annotation.parameter.expectation.exist";

  public static class Expectation {
    public enum Type { EXISTS, EXISTS_REGEX_MATCH }

    public String annotationName;
    public String parameter;
    public Type type;
    public String regex;

    public Expectation(String annotationName, String parameter, Type type, String regex) {
      this.annotationName = annotationName;
      this.parameter = parameter;
      this.type = type;
      this.regex = regex;
    }
  }

  private static final List<Expectation> execpectations =
      asList(new Expectation("FieldNameConstants", "innerTypeName", EXISTS_REGEX_MATCH, ""));

  @Override
  public int[] getDefaultTokens() {
    return new int[] {
        TokenTypes.ANNOTATION,
    };
  }

  @Override
  public int[] getRequiredTokens() {
    return new int[] {
        TokenTypes.ANNOTATION,
    };
  }

  @Override
  public int[] getAcceptableTokens() {
    return getDefaultTokens();
  }

  @Override
  public void visitToken(DetailAST annotationNode) {
    final String annotationNameCheck = AnnotationMixin.name(annotationNode);

    for (Expectation expectation : execpectations) {
      if (!annotationNameCheck.equals(expectation.annotationName)) {
        continue;
      }

      Map<String, DetailAST> parameters = AnnotationMixin.parameters(annotationNode);
      if (expectation.type == EXISTS || expectation.type == EXISTS_REGEX_MATCH) {
        if (!parameters.containsKey(expectation.parameter)) {
          log(annotationNode, MSG_EXIST_KEY, expectation.annotationName, expectation.parameter);
        }
      }
    }
  }
}