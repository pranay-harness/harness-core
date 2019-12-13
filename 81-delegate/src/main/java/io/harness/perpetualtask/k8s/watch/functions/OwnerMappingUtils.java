package io.harness.perpetualtask.k8s.watch.functions;

import com.google.api.client.util.Lists;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.harness.perpetualtask.k8s.watch.Owner;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@UtilityClass
class OwnerMappingUtils {
  static Owner getOwnerFromParent(HasMetadata hasMetadata) {
    if (hasMetadata.getMetadata().getOwnerReferences() != null) {
      Optional<OwnerReference> parentOwnerReference =
          hasMetadata.getMetadata().getOwnerReferences().stream().filter(Objects::nonNull).findFirst();

      if (parentOwnerReference.isPresent()) {
        return getOwner(parentOwnerReference.get().getUid(), parentOwnerReference.get().getName(),
            parentOwnerReference.get().getKind());
      }
    }
    return getOwnerFromMetaData(hasMetadata);
  }

  private static Owner getOwnerFromMetaData(HasMetadata hasMetadata) {
    return getOwner(hasMetadata.getMetadata().getUid(), hasMetadata.getMetadata().getName(), hasMetadata.getKind());
  }

  private static Owner getOwner(String uuid, String name, String kind) {
    return Owner.newBuilder().setUid(uuid).setName(name).setKind(kind).build();
  }

  static OwnerReference getOwnerReference(String kind, String name) {
    OwnerReference ownerReference = new OwnerReference();
    ownerReference.setUid(UUID.randomUUID().toString());
    ownerReference.setKind(kind);
    ownerReference.setName(name);
    return ownerReference;
  }

  static ObjectMeta getObjectMeta(String name, String parentKindName, String parentOwnerName) {
    ObjectMeta ownerObjectMeta = new ObjectMeta();
    ownerObjectMeta.setName(name);
    ownerObjectMeta.setOwnerReferences(getOwnerReferences(parentKindName, parentOwnerName));
    return ownerObjectMeta;
  }

  static List<OwnerReference> getOwnerReferences(String parentKindName, String parentOwnerName) {
    List<OwnerReference> ownerReferenceList = Lists.newArrayList();
    ownerReferenceList.add(getOwnerReference(parentKindName, parentOwnerName));
    return ownerReferenceList;
  }
}
