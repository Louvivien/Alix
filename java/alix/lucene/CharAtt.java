package alix.lucene;

import java.nio.CharBuffer;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.FutureObjects;

import alix.util.Chain;

/**
 * An implementation of CharTermAttribute for fast search in hash of strings.
 * 
 * @author fred
 *
 */
public class CharAtt extends AttributeImpl
    implements CharTermAttribute, TermToBytesRefAttribute, Cloneable, Comparable<CharAtt>
{
  /** Cached hashCode */
  private int hash;
  private static int MIN_BUFFER_SIZE = 10;

  private char[] chars;
  private int len = 0;

  /**
   * May be used by subclasses to convert to different charsets / encodings for
   * implementing {@link #getBytesRef()}.
   */
  protected BytesRefBuilder builder = new BytesRefBuilder();

  /** Initialize this attribute with empty term text */
  public CharAtt()
  {
    chars = new char[ArrayUtil.oversize(MIN_BUFFER_SIZE, Character.BYTES)];
  }

  /** Initialize the chars with a String */
  public CharAtt(String s)
  {
    len = s.length();
    this.chars = new char[len];
    s.getChars(0, len, this.chars, 0);
  }

  /** Initialize the chars with a Chain */
  public CharAtt(Chain chain)
  {
    len = chain.length();
    chars = new char[len];
    chain.getChars(chars);
  }

  @Override
  public final void copyBuffer(char[] buffer, int offset, int length)
  {
    growTermBuffer(length);
    System.arraycopy(buffer, offset, chars, 0, length);
    len = length;
  }

  @Override
  public final char[] buffer()
  {
    return chars;
  }

  @Override
  public final char[] resizeBuffer(int newSize)
  {
    if (chars.length < newSize) {
      // Not big enough; create a new array with slight
      // over allocation and preserve content
      final char[] newCharBuffer = new char[ArrayUtil.oversize(newSize, Character.BYTES)];
      System.arraycopy(chars, 0, newCharBuffer, 0, chars.length);
      chars = newCharBuffer;
    }
    return chars;
  }

  private void growTermBuffer(int newSize)
  {
    hash = 0;
    if (chars.length < newSize) {
      // Not big enough; create a new array with slight
      // over allocation:
      chars = new char[ArrayUtil.oversize(newSize, Character.BYTES)];
    }
  }

  @Override
  public final CharTermAttribute setLength(int length)
  {
    hash = 0;
    FutureObjects.checkFromIndexSize(0, length, chars.length);
    len = length;
    return this;
  }

  @Override
  public final CharTermAttribute setEmpty()
  {
    hash = 0;
    len = 0;
    return this;
  }

  public final boolean isEmpty()
  {
    return (len == 0);
  }
  // *** TermToBytesRefAttribute interface ***

  @Override
  public BytesRef getBytesRef()
  {
    builder.copyChars(chars, 0, len);
    return builder.get();
  }

  // *** CharSequence interface ***
  @Override
  public final int length()
  {
    return len;
  }

  @Override
  public final char charAt(int index)
  {
    FutureObjects.checkIndex(index, len);
    return chars[index];
  }

  @Override
  public final CharSequence subSequence(final int start, final int end)
  {
    FutureObjects.checkFromToIndex(start, end, len);
    return new String(chars, start, end - start);
  }

  // *** Appendable interface ***

  @Override
  public final CharTermAttribute append(CharSequence csq)
  {
    if (csq == null) // needed for Appendable compliance
      return appendNull();
    return append(csq, 0, csq.length());
  }

  @Override
  public final CharTermAttribute append(CharSequence csq, int start, int end)
  {
    if (csq == null) // needed for Appendable compliance
      csq = "null";
    // TODO: the optimized cases (jdk methods) will already do such checks, maybe
    // re-organize this?
    FutureObjects.checkFromToIndex(start, end, csq.length());
    final int length = end - start;
    if (length == 0) return this;
    resizeBuffer(this.len + length);
    if (length > 4) { // only use instanceof check series for longer CSQs, else simply iterate
      if (csq instanceof String) {
        ((String) csq).getChars(start, end, chars, this.len);
      }
      else if (csq instanceof StringBuilder) {
        ((StringBuilder) csq).getChars(start, end, chars, this.len);
      }
      else if (csq instanceof CharTermAttribute) {
        System.arraycopy(((CharTermAttribute) csq).buffer(), start, chars, this.len, length);
      }
      else if (csq instanceof CharBuffer && ((CharBuffer) csq).hasArray()) {
        final CharBuffer cb = (CharBuffer) csq;
        System.arraycopy(cb.array(), cb.arrayOffset() + cb.position() + start, chars, this.len, length);
      }
      else if (csq instanceof StringBuffer) {
        ((StringBuffer) csq).getChars(start, end, chars, this.len);
      }
      else {
        while (start < end)
          chars[this.len++] = csq.charAt(start++);
        // no fall-through here, as len is updated!
        return this;
      }
      this.len += length;
      return this;
    }
    else {
      while (start < end)
        chars[this.len++] = csq.charAt(start++);
      return this;
    }
  }

  @Override
  public final CharTermAttribute append(char c)
  {
    resizeBuffer(len + 1)[len++] = c;
    return this;
  }

  // *** For performance some convenience methods in addition to CSQ's ***

  @Override
  public final CharTermAttribute append(String s)
  {
    if (s == null) // needed for Appendable compliance
      return appendNull();
    final int length = s.length();
    s.getChars(0, length, resizeBuffer(this.len + length), this.len);
    this.len += length;
    return this;
  }

  @Override
  public final CharTermAttribute append(StringBuilder s)
  {
    if (s == null) // needed for Appendable compliance
      return appendNull();
    final int length = s.length();
    s.getChars(0, length, resizeBuffer(this.len + length), this.len);
    this.len += length;
    return this;
  }

  @Override
  public final CharTermAttribute append(CharTermAttribute ta)
  {
    if (ta == null) // needed for Appendable compliance
      return appendNull();
    final int length = ta.length();
    System.arraycopy(ta.buffer(), 0, resizeBuffer(this.len + length), this.len, length);
    len += length;
    return this;
  }

  private CharTermAttribute appendNull()
  {
    resizeBuffer(len + 4);
    chars[len++] = 'n';
    chars[len++] = 'u';
    chars[len++] = 'l';
    chars[len++] = 'l';
    return this;
  }

  @Override
  public void clear()
  {
    hash = 0;
    len = 0;
  }

  @Override
  public CharAtt clone()
  {
    CharAtt t = (CharAtt) super.clone();
    // Do a deep clone
    t.chars = new char[this.len];
    System.arraycopy(this.chars, 0, t.chars, 0, this.len);
    t.builder = new BytesRefBuilder();
    t.builder.copyBytes(builder.get());
    return t;
  }

  @Override
  public boolean equals(Object other)
  {
    if (other == this) {
      return true;
    }
    int len = this.len;
    char[] chars = this.chars;
    if (other instanceof CharAtt) {
      CharAtt term = (CharAtt) other;
      if (term.len != len) return false;
      // if hashcode already calculated, if different, not same strings
      if (hash != 0 && term.hash != 0 && hash != term.hash) return false;
      char[] test = term.chars;
      for (int i = 0; i < len; i++) {
        if (test[i] != chars[i]) return false;
      }
      return true;
    }
    // String or other CharSequence, access char by char
    else if (other instanceof CharSequence) {
      CharSequence cs = (CharSequence) other;
      if (cs.length() != len) return false;
      for (int i = 0; i < len; i++) {
        if (cs.charAt(i) != chars[i]) return false;
      }
      return true;
    }
    if (other instanceof Chain) {
      Chain chain = (Chain) other;
      if (chain.length() != len) return false;
      char[] test = chain.array();
      int start = chain.start();
      for (int i = 0; i < len; i++) {
        if (test[start] != chars[i]) return false;
        start++;
      }
      return true;
    }
    else if (other instanceof char[]) {
      char[] test = (char[]) other;
      if (test.length != len) return false;
      for (int i = 0; i < len; i++) {
        if (test[i] != chars[i]) return false;
      }
      return true;
    }
    else return false;
  }
  
  public void setCharAt(int pos, char c)
  {
    hash = 0;
    chars[pos] = c;
  }

  /**
   * Returns solely the term text as specified by the {@link CharSequence}
   * interface.
   */
  @Override
  public String toString()
  {
    return new String(chars, 0, len);
  }

  @Override
  public void reflectWith(AttributeReflector reflector)
  {
    reflector.reflect(CharTermAttribute.class, "term", toString());
    reflector.reflect(TermToBytesRefAttribute.class, "bytes", getBytesRef());
  }

  @Override
  public void copyTo(AttributeImpl target)
  {
    CharTermAttribute t = (CharTermAttribute) target;
    t.copyBuffer(chars, 0, len);
  }

  /**
   * Same hashCode() as a String computed as <blockquote>
   * 
   * <pre>
   * s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
   * </pre>
   * 
   * </blockquote> using <code>int</code> arithmetic, where <code>s[i]</code> is
   * the <i>i</i>th character of the string, <code>n</code> is the length of the
   * string, and <code>^</code> indicates exponentiation. (The hash value of the
   * empty string is zero.)
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode()
  {
    int h = hash;
    if (h == 0) {
      char[] chars = this.chars;
      int end = len;
      for (int i = 0; i < end; i++) {
        h = 31 * h + chars[i];
      }
      hash = h;
    }
    return h;
  }

  public int compareTo(String string)
  {
    char[] chars = this.chars;
    int lim = Math.min(len, string.length());
    for (int offset = 0; offset < lim; offset++) {
      char c1 = chars[offset];
      char c2 = string.charAt(offset);
      if (c1 != c2) {
        return c1 - c2;
      }
    }
    return len - string.length();
  }

  @Override
  public int compareTo(CharAtt o)
  {
    char[] chars1 = chars;
    char[] chars2 = o.chars;
    int lim = Math.min(len, o.len);
    for (int offset = 0; offset < lim; offset++) {
      char c1 = chars1[offset];
      char c2 = chars2[offset];
      if (c1 != c2) {
        return c1 - c2;
      }
    }
    return 0;
  }

}
