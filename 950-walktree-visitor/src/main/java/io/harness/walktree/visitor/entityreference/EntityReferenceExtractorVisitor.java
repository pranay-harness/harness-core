package io.harness.walktree.visitor.entityreference;

import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.ParentQualifier;
import io.harness.walktree.beans.VisitElementResult;
import io.harness.walktree.visitor.DummyVisitableElement;
import io.harness.walktree.visitor.SimpleVisitor;
import io.harness.walktree.visitor.utilities.VisitorParentPathUtils;

import com.google.inject.Injector;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.NotImplementedException;

public class EntityReferenceExtractorVisitor extends SimpleVisitor<DummyVisitableElement> {
  Set<EntityDetailProtoDTO> entityReferenceSet;
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;

  public Set<EntityDetailProtoDTO> getEntityReferenceSet() {
    return entityReferenceSet;
  }

  public EntityReferenceExtractorVisitor(Injector injector, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, List<LevelNode> initialLevelNodes) {
    super(injector);
    entityReferenceSet = new HashSet<>();
    this.accountIdentifier = accountIdentifier;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    if (initialLevelNodes != null) {
      initialLevelNodes.forEach(levelNode -> VisitorParentPathUtils.addToParentList(this.getContextMap(), levelNode));
    }
  }

  @Override
  public VisitElementResult preVisitElement(Object element) {
    // add to the list of parent.
    if (element instanceof ParentQualifier) {
      LevelNode levelNode = ((ParentQualifier) element).getLevelNode();
      VisitorParentPathUtils.addToParentList(this.getContextMap(), levelNode);
    }
    return super.preVisitElement(element);
  }

  @Override
  public VisitElementResult postVisitElement(Object element) {
    // Remove from parent list once traversed
    if (element instanceof ParentQualifier) {
      VisitorParentPathUtils.removeFromParentList(this.getContextMap());
    }
    return super.postVisitElement(element);
  }

  @Override
  public VisitElementResult visitElement(Object currentElement) {
    DummyVisitableElement helperClassInstance = getHelperClass(currentElement);
    if (helperClassInstance == null) {
      throw new NotImplementedException("Helper Class not implemented for object of type" + currentElement.getClass());
    }
    if (helperClassInstance instanceof EntityReferenceExtractor) {
      EntityReferenceExtractor entityReferenceExtractor = (EntityReferenceExtractor) helperClassInstance;
      Set<EntityDetailProtoDTO> newReferences = entityReferenceExtractor.addReference(
          currentElement, accountIdentifier, orgIdentifier, projectIdentifier, this.getContextMap());
      if (hasSome(newReferences)) {
        entityReferenceSet.addAll(newReferences);
      }
    }
    return VisitElementResult.CONTINUE;
  }
}
