package site.oeuvres.util;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A mutable string implementation thought for efficiency more than security.
 * The hash function is same as String so that a Term can be found in a Set<String>.
 * The same internal char array could be shared by multiple Term instances (with different offset and len),
 * allowing to scan 
 * 
 * @author glorieux-f
 */
public class Term implements CharSequence, Comparable<Term>
{
  /** The characters */
  private char[] data = new char[16];
  /** Start index of the String in the array, allow fast trim(), or allow different terms to share the same text line (ex: char separated values)  */
  private int start = 0;
  /** Number of characters used */
  private int len;
  /** Cache the hash code for the string */
  private int hash; // Default to 0
  /** Internal pointer inside the scope of the string, used by csv scanner (value()) */
  private int pointer = -1;
  // boolean lock;
  
  /**
   * Empty constructor, value will be set later
   */
  public Term( )
  {
  }
  /**
   * Construct a term by copy of a term content (modification of this Term object will NOT affect source Term)
   */
  public Term( Term t ) 
  {
    replace(t);
  }
  /**
   * Construct a term by copy of a char array  (modification of this Term object will NOT affect the source array)
   * @param a a char array
   */
  public Term( final char[] a ) 
  {
    replace(a, -1, -1);
  }
  /**
   * Construct a term by copy of a char sequence 
   * @param cs a char sequence (String, but also String buffers or builders)
   */
  public Term( CharSequence cs ) 
  {
    replace( cs, -1, -1 );
  }
  /*
   * Construct a term by copy of a section of char sequence (String, but also String buffers or builders)
   * @param cs a char sequence (String, but also String buffers or builders)
   * @param offset start index from source string
   * @param count number of chars from offset
   */
  public Term( CharSequence cs, int offset, int count ) 
  {
    replace( cs, offset, count );
  }
  
  @Override
  public int length()
  {
    return len;
  }
  /**
   * Link by reference to a section of a term (modification of one of the Term WILL the 2 Term)
   * @param term the source term
   * @param offset start index from source term 
   * @param count number of chars from offset
   * @return the Term object for chaining
   */
  public Term link( final Term term, final int offset, final int count )
  {
    data = term.data;
    start = term.start + offset;
    len = count;
    return this;
  }
  /**
   * Link a term by reference to a section of a char array (modification of the Term object or the char array WILL affect the other)
   * @param a a char array
   * @param offset start index from source term 
   * @param count number of chars from offset
   */
  public Term link( final char[] a, final int offset, final int count ) 
  {
    data = a;
    start = offset;
    len = count;
    return this;
  }
  /**
   * Is Term with no chars ?
   * @return true if len == 0, or false
   */
  public boolean isEmpty()
  {
    return (len == 0);
  }

  @Override
  public char charAt( int index )
  {
    if ((index < 0) || (index >= len)) {
        throw new StringIndexOutOfBoundsException(index);
    }
    return data[start+index];
  }
  /**
   * Reset term (no modification to internal data)
   * @return the Term
   */
  public Term clear()
  {
    len = 0;
    hash = 0;
    return this;
  }
  /**
   * Replace Term content by copy af a String
   * @param cs a char sequence
   * @return the Term object for chaining
   */
  public Term replace( CharSequence cs  ) 
  {
    return replace( cs, -1, -1);
  }
  /**
   * Replace Term content by a span of String
   * @param cs a char sequence
   * @param index of the string from where to copy chars  
   * @param number of chars to copy -1
   * @return the Term object for chaining, or null if the String provided is null (for testing)
   */
  public Term replace( CharSequence cs, int offset, int count  ) 
  {
    if ( cs == null ) return null;
    if (offset <= 0 && count < 0) {
      offset = 0;
      count = cs.length();
    }
    if ( count <= 0) {
      len = 0;
      return this;
    }
    sizing( count );
    for (int i=start; i<count; i++) {
      data[i] = cs.charAt( offset++ );
    }
    len = count;
    /*
    // longer
    value = s.toCharArray();
    len = value.length;
    */
    return this;
  }
  /**
   * Replace Term content by copy of a char array,  2 time faster than String
   * @param a text as char array
   * @return the Term object for chaining
   */
  public Term replace( char[] a  ) 
  {
    return replace( a, -1, -1);
  }
  /**
   * Replace Term content by a span of a char array
   * @param a text as char array
   * @param index of the string from where to copy chars  
   * @param number of chars to copy -1
   * @return the Term object for chaining
   */
  public Term replace( char[] a, int offset, int count ) 
  {
    if (offset <= 0 && count < 0) {
      offset = 0;
      count = a.length;
    }
    if ( count <= 0) {
      len = 0;
      return this;
    }
    len = count;
    sizing( count );
    System.arraycopy( a, offset, data, start, count );
    return this;
  }
  /**
   * Replace this term content by the copy of a term.
   * Reset the start position.
   * @param term
   * @return
   */
  public Term replace( Term term ) 
  {
    int newlen = term.len;
    // do not change len before sizing
    sizing( newlen );
    System.arraycopy( term.data, term.start, data, start, newlen );
    len = newlen;
    return this;
  }

  /**
   * Append a character
   * @param c
   * @return the Term object for chaining
   */
  public Term append( char c )
  {
    int newlen = len + 1;
    sizing( newlen );
    data[len] = c;
    len = newlen;
    return this;
  }
  /**
   * Append a term
   * @param term
   * @return the Term object for chaining
   */
  public Term append( Term term )
  {
    int newlen = len + term.len;
    sizing( newlen );
    System.arraycopy( term.data, term.start, data, start+len, term.len);
    len = newlen;
    return this;
  }
  /**
   * Append a string
   * @param cs String or other CharSequence
   * @return the Term object for chaining
   */
  public Term append( CharSequence cs )
  {
    int count = cs.length();
    int newlen = len + count;
    sizing( newlen );
    int offset = start+len;
    for (int i=0; i<count; i++) {
      data[offset++] = cs.charAt( i );
    }
    len = newlen;
    return this;
  }
  
  /**
   * Test if char array container is big enough to contain a new size 
   * If resized, be nice, keep chars outside the scope of this term, maybe they are needed by
   * other users of the char array
   * @param newlen The new size to put in
   * @return true if resized
   */
  private boolean sizing( final int newlen )
  {
    hash = 0; // resizing occurs when content change, uncache hashcode
    if ( (start+newlen) <= data.length ) return false;
    char[] a = new char[ Calcul.square2( start+newlen ) ];
    try {
      System.arraycopy( data, 0, a, 0, data.length );
    } catch (Exception e) {
      System.out.println( new String( data )+" "+len );
      throw(e);
    }
    data = a;
    return true;
  }

  /**
   * Last char, will send an array out of bound, with no test
   * @return last char
   */
  public char last()
  {
    return data[len-1];
  }
  /** 
   * Set last char, will send an array out of bound, with no test
   */
  public Term last( char c )
  {
    data[len-1] = c;
    hash = 0;
    return this;
  }
  /**
   * Remove last char
   */
  public Term lastDel()
  {
    len--;
    hash = 0;
    return this;
  }
  /** 
   * First char 
   */
  public char first()
  {
    return data[0];
  }
  /**
   * Change case of the chars in scope of the term.
   * @return the Term object for chaining
   */
  public Term toLower()
  {
    char c;
    for ( int i=start; i < len; i++ ) {
      c = data[i];
      if ( Char.isLowerCase( c ) ) continue;
      // a predefine hash of chars is not faster
      // if ( LOWER.containsKey( c )) s.setCharAt( i, LOWER.get( c ) );
      data[i] = Character.toLowerCase( c );
    }
    hash = 0;
    return this;
  }
  /**
   * Suppress spaces at start and end of string.
   * Do not affect the internal char array data but modify only its limits.
   * Should be very efficient.
   * @return the modified Term object
   */
  public Term trim()
  {
    int from = start;
    char[] dat = data;
    int to = start + len;
    while ( from < to && dat[from] < ' ' ) from++;
    to --;
    while ( to > from && dat[to] < ' ' ) to--;
    start = from;
    len = to -from + 1;
    return this;
  }
  /**
   * Suppress specific chars at start and end of String.
   * Do not affect the internal char array data but modify only its limits.
   * @param chars
   * @return the modified Term object
   */
  public Term trim( String chars)
  {
    int from = start;
    char[] dat = data;
    int to = start + len;
    // possible optimisation on indexOf() ?
    while ( from < to && chars.indexOf( dat[from] ) > -1 ) from++;
    to --;
    // test for escape chars ? "\""
    while ( to > from && chars.indexOf( dat[to] ) > -1 ) to--;
    start = from;
    len = to -from + 1;
    return this;
  }
  
  public int value( Term cell, final char separator )
  {
    if ( pointer < 0 ) pointer = 0;
    pointer = value( cell, separator, pointer );
    return pointer;
  }
  
  /**
   * Read value in a char separated string. The term objected is update from offset position to the next separator (or end of String).
   * @param separator
   * @param offset index position to read from
   * @return a new offset from where to search in String, or -1 when end of line is reached
   */
  public int value( Term cell, final char separator, final int offset )
  {
    if ( offset >= len ) return -1;
    char[] dat = data;
    int to = start + offset;
    int max = start + len;
    while ( to < max) {
      if ( dat[to] == separator && (to == 0 || dat[to-1] != '\\') ) { // test escape char
        cell.link( this, offset, to-offset-start );
        return to-start+1;
      }
      to++;
    }
    // end of line
    cell.link( this, offset, to-offset-start );
    return to-start;
  }
  
  /**
   * An split on one char
   * @param separator
   * @return
   */
  public String[] split( char separator )
  {
    // store generated Strings in alist
    ArrayList<String> list = new ArrayList<>();
    int offset = start;
    int to = start;
    int max = start + len;
    char[] dat = data;
    while ( to <= max ) {
      // not separator, continue
      if ( dat[to] != separator ) {
        to++;
        continue;
      }
      // separator, add a String
      list.add( new String( dat, offset, to-offset) );
      offset = ++to;
    }
    list.add( new String( dat, offset, to-offset) );
    return list.toArray( new String[0] );
  }
  
  /**
   * Modify a char in the String
   * @param index position of the char to change
   * @param c new char value
   * @return the Term object for chaining
   */
  public Term setCharAt( int index, char c )
  {
    if ( (index < 0) || (index >= len) ) {
        throw new StringIndexOutOfBoundsException(index);
    }
    data[start+index] = c;
    return this;
  }
  /**
   * Test prefix
   * @param prefix
   * @return true if the Term starts by prefix
   */
  public boolean startsWith( final CharSequence prefix)
  {
    int lim = prefix.length();
    if ( lim > len) return false;
    for ( int i=0; i < lim; i++ ) {
      if ( prefix.charAt( i ) != data[start+i]) return false;
    }
    return true;
  }
  @Override
  public CharSequence subSequence( int start, int end )
  {
    System.out.println( "Term.subSequence() TODO Auto-generated method stub" );
    return null;
  }
  
  @Override
  public boolean equals ( Object o ) {
    // System.out.println( this+" =? "+o );
    if (this == o) return true;
    char[] test;
    // limit field lookup
    int offset = start;
    int lim = len;
    if (o instanceof Term) {
      if ( ((Term)o).len != lim ) return false;
      test = ((Term)o).data;
      int offset2 = ((Term)o).start;
      for ( int i=0 ; i < lim ; i++ ) {
        if ( data[offset+i] != test[offset2+i] ) return false;
      }
      return true;
    }
    else if (o instanceof char[]) {
      if ( ((char[])o).length != lim ) return false;
      test = (char[])o;
      for ( int i=0; i < lim; i++ ) {
        if ( data[offset+i] != test[i] ) return false;
      }
      return true;
    }
    // String or other CharSequence, by acces char by char
    // faster than copy of the compared char array, even for complete equals
    else if ( o instanceof CharSequence ) {
      if ( ((CharSequence)o).length() != len) return false;
      for ( int i=0; i < lim; i++ ) {
        if ( ((CharSequence)o).charAt( i ) != data[offset+i]) return false;
      }
      return true;
    }
    /*
    else if (o instanceof String) {
      if ( ((String)o).length() != len) return false;
      test = ((String)o).toCharArray();
      for ( int i=0; i < len; i++ ) {
        if ( data[offset+i] != test[i] ) return false;
      }
      return true;
    }
    */
    else return false;
  }
  /**
   * HashMaps maybe optimized by ordered lists in buckets
   * Do not use as a nice orthographic ordering
   */
  @Override
  public int compareTo( Term t ) {
    char v1[] = data;
    char v2[] = t.data;
    int k1 = start;
    int k2 = t.start;
    int lim1 = k1 + Math.min( len, t.len);
    while ( k1 < lim1 ) {
        char c1 = v1[k1];
        char c2 = v2[k2];
        if (c1 != c2) {
            return c1 - c2;
        }
        k1++;
        k2++;
    }
    return len - t.len;
  }

  /**
   * Returns a hash code for this string. The hash code for a
   * <code>String</code> object is computed as
   * <blockquote><pre>
   * s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
   * </pre></blockquote>
   * using <code>int</code> arithmetic, where <code>s[i]</code> is the
   * <i>i</i>th character of the string, <code>n</code> is the length of
   * the string, and <code>^</code> indicates exponentiation.
   * (The hash value of the empty string is zero.)
   *
   * @return  a hash code value for this object.
   */
  @Override
  public int hashCode() {
    int h = hash;
    if (h == 0) {
      int end = start+len;
      for (int i = start; i < end; i++) {
        h = 31*h + data[i];
      }
      hash = h;
    }
    // System.out.println( this+" "+hash );
    return h;
  }
  @Override
  public String toString( )
  {
    // for debug only
    // System.out.println( "length="+data.length+" start="+start+" len="+len );
    return new String( data, start+0, len );
  }
  /**
   * No reason to use in CLI, except for testing.
   * @param args
   */
  public static void main(String[] args)
  {
    Term term;
    String s = null;
    term = new Term(s);
    Term line = new Term(",,C,D,,EPO");
    System.out.println( line+" test CSV" );
    Term cell = new Term();
    while ( line.value( cell, ',' ) > -1 ) {
      System.out.print( cell );
      System.out.print( '|' );
    }
    System.out.println( );
    System.out.println( Arrays.toString( line.split( ',' ) ) );
    System.out.println( "trim() \"     \" \""+ new Term("     ").trim( " " )+"\"" ); 
    System.out.println( "// Illustration of char data shared between two terms" );
    line = new Term("01234567890123456789");
    System.out.println( "line: \""+line+"\"" );
    Term span = new Term( );
    span.link( line, 3, 4 );
    System.out.println( "span=new Term(line, 3, 4): \""+span+"\"" );
    span.setCharAt( 0, '-' );
    System.out.println( "span.setCharAt(0, '-') " );
    System.out.println( "line: \""+line+"\"" );
    System.out.println( "span: \""+span+"\"" );
    System.out.println( "Test comparesTo()="+span.compareTo( new Term("-456") )+" equals()="+span.equals( "-456" ) );
    System.out.println( span.append( "____" )+", "+line );
    for ( char c=33; c < 100; c++) span.append( c );
    System.out.println( "span: \""+span+"\"" );
    System.out.println( "line: \""+line+"\"" );
    System.out.print( "Testing equals()" );
    long time = System.nanoTime();
    // test equals perf with a long String
    String text = "java - CharBuffer vs. char[] - Stack Overflow stackoverflow.com/questions/294382/charbuffer-vs-char Traduire cette page 16 nov. 2008 - No, there's really no reason to prefer a CharBuffer in this case. In general, though ..... P.S If you use a backport remember to remove it once you catch up to the version containing the real version of the backported code.";
    term = new Term(text);
    term.last( 'p' ); // modify the last char
    for ( int i=0; i < 10000000; i++) { 
      term.equals( text );
    }
    System.out.print( term.equals( text ) );
    System.out.println( " " + ((System.nanoTime() - time) / 1000000) + " ms");

    /*
    System.out.println( t.copy( "abcde", 2, 1 ) );
    t.clear();
    for ( char c=33; c < 100; c++) t.append( c );
    System.out.println( t );
    t.clear();
    for ( int i=0; i < 100; i++) {
      t.append( " "+i );
    }
    System.out.println( t );
    */
    /*
    HashSet<String> dic = new HashSet<String>();
    String text="de en le la les un une des";
    for (String token: text.split( " " )) {
      dic.add( token );
    }
    System.out.println( dic );
    System.out.println( "HashSet<String>.contains(String) "+dic.contains( "des" ) );
    System.out.println( "HashSet<String>.contains(Term) "+dic.contains( new Term("des") ) );
    */
  }
}
