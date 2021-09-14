/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.app;

import io.harness.exception.UnsupportedOperationException;

import com.github.rutledgepaulv.qbuilders.nodes.ComparisonNode;
import com.github.rutledgepaulv.qbuilders.visitors.MongoVisitor;
import java.util.Set;
import org.springframework.data.mongodb.core.query.Criteria;

public class ConstrainedMongoVisitor extends MongoVisitor {
  private final Set<String> annotatedFields;

  public ConstrainedMongoVisitor(Set<String> annotatedFields) {
    this.annotatedFields = annotatedFields;
  }

  @Override
  public Criteria visit(ComparisonNode node) {
    String field = node.getField().asKey();
    if (!annotatedFields.contains(field)) {
      throw new UnsupportedOperationException(String.format("Filtering on field %s is not supported", field));
    }
    return super.visit(node);
  }
}
