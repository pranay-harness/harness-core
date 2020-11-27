package io.harness.walktree.visitor.entityreference;

import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.EntityDetail;
import io.harness.walktree.beans.VisitElementResult;
import io.harness.walktree.visitor.DummyVisitableElement;
import io.harness.walktree.visitor.SimpleVisitor;

import com.google.inject.Injector;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.NotImplementedException;

public class EntityReferenceExtractorVisitor extends SimpleVisitor<DummyVisitableElement> {
  Set<EntityDetail> entityReferenceSet;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;

  public Set<EntityDetail> getEntityReferenceSet() {
    return entityReferenceSet;
  }

  public EntityReferenceExtractorVisitor(
      Injector injector, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    super(injector);
    entityReferenceSet = new HashSet<>();
    this.accountIdentifier = accountIdentifier;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
  }

  @Override
  public VisitElementResult visitElement(Object currentElement) {
    DummyVisitableElement helperClassInstance = getHelperClass(currentElement);
    if (helperClassInstance == null) {
      throw new NotImplementedException("Helper Class not implemented for object of type" + currentElement.getClass());
    }
    if (helperClassInstance instanceof EntityReferenceExtractor) {
      EntityReferenceExtractor entityReferenceExtractor = (EntityReferenceExtractor) helperClassInstance;
      Set<EntityDetail> newReferences =
          entityReferenceExtractor.addReference(currentElement, accountIdentifier, orgIdentifier, projectIdentifier);
      if (EmptyPredicate.isNotEmpty(newReferences)) {
        entityReferenceSet.addAll(newReferences);
      }
    }
    return VisitElementResult.CONTINUE;
  }
}
