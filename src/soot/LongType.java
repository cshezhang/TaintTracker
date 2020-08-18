package soot;

import soot.util.Switch;

/**
 * Soot representation of the Java built-in type 'long'. Implemented as a singleton.
 */
@SuppressWarnings("serial")
public class LongType extends PrimType {
  public LongType(Singletons.Global g) {
  }

  public static LongType v() {
    return G.v().soot_LongType();
  }

  public boolean equals(Object t) {
    if(t instanceof LongType) {
      return this.hashCode() == t.hashCode();
    }
    return false;
//    return this == t;
  }

  public int hashCode() {
    return 0x023DA077;
  }

  public String toString() {
    return "long";
  }

  public void apply(Switch sw) {
    ((TypeSwitch) sw).caseLongType(this);
  }

  @Override
  public RefType boxedType() {
    return RefType.v("java.lang.Long");
  }
}
