package io.harness.rule;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;

public class AuthorRule extends RepeatRule {
  private static final Logger logger = LoggerFactory.getLogger(AuthorRule.class);

  private static List<String> active = asList("george@harness.io", "raghu@harness.io", "sriram@harness.io");

  @Retention(RetentionPolicy.RUNTIME)
  @Target({java.lang.annotation.ElementType.METHOD})
  public @interface Author {
    String[] emails();
    boolean intermittent() default false;
  }

  @Override
  public Statement apply(Statement statement, Description description) {
    Author author = description.getAnnotation(Author.class);
    if (author == null) {
      return statement;
    }

    for (String email : author.emails()) {
      if (!active.contains(email)) {
        throw new RuntimeException(format("Email %s is not active.", email));
      }
    }

    // If there is email, it should match
    final String ghprbActualCommitAuthorEmail = System.getenv("ghprbActualCommitAuthorEmail");
    if (ghprbActualCommitAuthorEmail == null) {
      return statement;
    }

    logger.info("ghprbActualCommitAuthorEmail = {}", ghprbActualCommitAuthorEmail);

    final boolean match = Arrays.stream(author.emails()).anyMatch(email -> email.equals(ghprbActualCommitAuthorEmail));
    if (!match) {
      if (author.intermittent()) {
        return RepeatRule.RepeatStatement.builder().build();
      }
      return statement;
    }

    return RepeatRule.RepeatStatement.builder()
        .statement(statement)
        .parentRule(this)
        .times(20)
        .successes(20)
        .timeoutOnly(true)
        .build();
  }
  }
