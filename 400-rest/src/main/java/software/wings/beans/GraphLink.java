/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

/**
 * The Class Link.
 */
public class GraphLink {
  private String id;
  private String from;
  private String to;
  private String type;

  /**
   * Gets id.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets id.
   *
   * @param id the id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets from.
   *
   * @return the from
   */
  public String getFrom() {
    return from;
  }

  /**
   * Sets from.
   *
   * @param from the from
   */
  public void setFrom(String from) {
    this.from = from;
  }

  /**
   * Gets to.
   *
   * @return the to
   */
  public String getTo() {
    return to;
  }

  /**
   * Sets to.
   *
   * @param to the to
   */
  public void setTo(String to) {
    this.to = to;
  }

  /**
   * Gets type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets type.
   *
   * @param type the type
   */
  public void setType(String type) {
    this.type = type;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((from == null) ? 0 : from.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((to == null) ? 0 : to.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    GraphLink other = (GraphLink) obj;
    if (from == null) {
      if (other.from != null) {
        return false;
      }
    } else if (!from.equals(other.from)) {
      return false;
    }
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    if (to == null) {
      if (other.to != null) {
        return false;
      }
    } else if (!to.equals(other.to)) {
      return false;
    }
    if (type == null) {
      if (other.type != null) {
        return false;
      }
    } else if (!type.equals(other.type)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Link [id=" + id + ", from=" + from + ", to=" + to + ", type=" + type + "]";
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private String id;
    private String from;
    private String to;
    private String type;

    private Builder() {}

    /**
     * A link.
     *
     * @return the builder
     */
    public static Builder aLink() {
      return new Builder();
    }

    /**
     * With id.
     *
     * @param id the id
     * @return the builder
     */
    public Builder withId(String id) {
      this.id = id;
      return this;
    }

    /**
     * With from.
     *
     * @param from the from
     * @return the builder
     */
    public Builder withFrom(String from) {
      this.from = from;
      return this;
    }

    /**
     * With to.
     *
     * @param to the to
     * @return the builder
     */
    public Builder withTo(String to) {
      this.to = to;
      return this;
    }

    /**
     * With type.
     *
     * @param type the type
     * @return the builder
     */
    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    /**
     * But.
     *
     * @return the builder
     */
    public Builder but() {
      return aLink().withId(id).withFrom(from).withTo(to).withType(type);
    }

    /**
     * Builds the.
     *
     * @return the link
     */
    public GraphLink build() {
      GraphLink link = new GraphLink();
      link.setId(id);
      link.setFrom(from);
      link.setTo(to);
      link.setType(type);
      return link;
    }
  }
}
