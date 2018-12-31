package io.harness.queue;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Reference;

import java.util.Objects;

public class TestQueuableWithEntity extends Queuable {
  @Reference private TestInternalEntity entity;

  /**
   * Instantiates a new test queuable with entity.
   */
  public TestQueuableWithEntity() {}

  /**
   * Instantiates a new test queuable with entity.
   *
   * @param other the other
   */
  public TestQueuableWithEntity(TestQueuableWithEntity other) {
    super(other);
    this.entity = other.entity;
  }

  /**
   * Instantiates a new test queuable with entity.
   *
   * @param entity the entity
   */
  public TestQueuableWithEntity(TestInternalEntity entity) {
    this.entity = entity;
  }

  /**
   * Gets entity.
   *
   * @return the entity
   */
  public TestInternalEntity getEntity() {
    return entity;
  }

  /**
   * Sets entity.
   *
   * @param entity the entity
   */
  public void setEntity(TestInternalEntity entity) {
    this.entity = entity;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    TestQueuableWithEntity that = (TestQueuableWithEntity) obj;
    return Objects.equals(entity, that.entity);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(entity);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("entity", entity).toString();
  }
}
