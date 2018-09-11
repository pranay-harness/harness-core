package io.harness.rule;

import io.harness.rule.RepeatRule.RepeatStatement.RepeatStatementBuilder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

public class AuthorRule extends RepeatRule {
  private static final Logger logger = LoggerFactory.getLogger(AuthorRule.class);

  @Retention(RetentionPolicy.RUNTIME)
  @Target({java.lang.annotation.ElementType.METHOD})
  public @interface Author {
    String name() default "";
    String email();
  }

  @Override
  public Statement apply(Statement statement, Description description) {
    final RepeatStatementBuilder builder = RepeatRule.RepeatStatement.builder().statement(statement).parentRule(this);

    Author author = description.getAnnotation(Author.class);
    if (author == null) {
      return statement;
    }

    // If there is email, it should match
    if (!author.email().isEmpty()) {
      final String ghprbActualCommitAuthorEmail = System.getenv("ghprbActualCommitAuthorEmail");
      logger.info("ghprbActualCommitAuthorEmail = {}", ghprbActualCommitAuthorEmail);

      if (!Objects.equals(author.email(), ghprbActualCommitAuthorEmail)) {
        return statement;
      }
    } else if (!author.name().isEmpty()) {
      final String ghprbActualCommitAuthor = System.getenv("ghprbActualCommitAuthor");
      logger.info("ghprbActualCommitAuthor = {}", ghprbActualCommitAuthor);

      if (!Objects.equals(author.name(), ghprbActualCommitAuthor)) {
        return statement;
      }
    } else {
      throw new RuntimeException("Either author email or name should be set");
    }
    return builder.times(20).successes(20).timeoutOnly(true).build();
  }
  }
