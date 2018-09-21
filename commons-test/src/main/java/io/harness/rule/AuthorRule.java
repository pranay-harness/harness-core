package io.harness.rule;

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
    boolean intermittent() default false;
  }

  @Override
  public Statement apply(Statement statement, Description description) {
    Author author = description.getAnnotation(Author.class);
    if (author == null) {
      return statement;
    }

    // If there is email, it should match
    if (!author.email().isEmpty()) {
      final String ghprbActualCommitAuthorEmail = System.getenv("ghprbActualCommitAuthorEmail");
      logger.info("ghprbActualCommitAuthorEmail = {}", ghprbActualCommitAuthorEmail);

      if (!Objects.equals(author.email(), ghprbActualCommitAuthorEmail)) {
        if (author.intermittent()) {
          return RepeatRule.RepeatStatement.builder().build();
        }
        return statement;
      }
    } else if (!author.name().isEmpty()) {
      final String ghprbActualCommitAuthor = System.getenv("ghprbActualCommitAuthor");
      logger.info("ghprbActualCommitAuthor = {}", ghprbActualCommitAuthor);

      if (!Objects.equals(author.name(), ghprbActualCommitAuthor)) {
        if (author.intermittent()) {
          return RepeatRule.RepeatStatement.builder().build();
        }
        return statement;
      }
    } else {
      throw new RuntimeException("Either author email or name should be set");
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
