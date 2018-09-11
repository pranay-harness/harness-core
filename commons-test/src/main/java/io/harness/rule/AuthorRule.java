package io.harness.rule;

import io.harness.rule.RepeatRule.RepeatStatement.RepeatStatementBuilder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

public class AuthorRule extends RepeatRule {
  @Retention(RetentionPolicy.RUNTIME)
  @Target({java.lang.annotation.ElementType.METHOD})
  public @interface Author {
    String name();
  }

  @Override
  public Statement apply(Statement statement, Description description) {
    final RepeatStatementBuilder builder = RepeatRule.RepeatStatement.builder().statement(statement).parentRule(this);

    Author author = description.getAnnotation(Author.class);
    final String ghprbActualCommitAuthor = System.getenv("ghprbActualCommitAuthor");

    if (author != null && Objects.equals(author.name(), ghprbActualCommitAuthor)) {
      return builder.times(20).successes(20).timeoutOnly(true).build();
    }
    return statement;
  }
  }
