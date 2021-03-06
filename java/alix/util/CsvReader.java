package alix.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.HashMap;

import alix.fr.dic.Tag;
import alix.lucene.CharAtt;
import alix.lucene.CharDic.LexEntry;

/**
 * A light fast csv parser without Strings, especially to load jar resources.
 * Populate a reusable predefined array of object.
 * @author fred
 *
 */
public class CsvReader
{
  private static final int BUFFER_SIZE = 16384; // tested
  private final char[] buf = new char[BUFFER_SIZE];
  private int bufPos;
  private int bufLen;
  private static final char LF = '\n';
  private static final char CR = '\r';
  /** Read all chars at once */
  private String chars;
  private int len;
  private int pos;
  /** The char source */
  private Reader reader;
  /** The cell delimiter char */
  private final char sep;
  /** The text delimiter char */
  private final char quote;
  /** Row to populate */
  private Row row;
  /** line number */
  private int line = -1;


  public CsvReader(Reader reader, int cols) throws IOException
  {
    this(reader, cols, ';');
  }
  public CsvReader(Reader reader, int cols, char sep) throws IOException
  {
    this.reader = reader;
    row = new Row(cols);
    quote = '"';
    this.sep = sep;
  }
  public Row row()
  {
    return this.row;
  }
  public int line()
  {
    return this.line;
  }

  public boolean readRow() throws IOException {
    if (this.bufPos < 0) return false;
    Row row = this.row.reset();
    Chain cell = row.next();
    int bufPos = this.bufPos;
    int bufMark = bufPos; // from where to start a copy
    char sep = this.sep;
    char quote = this.quote;
    boolean inquote;
    char lastChar = 0;
    int crlf = 0; // used to not append CR to a CRLF ending line 
    while (true) {
      // fill buffer
      if (bufLen == bufPos) {
        // copy chars before erase them
        if (cell == null);
        else if (lastChar == CR) cell.append(buf, bufMark, bufLen - bufMark -1 ); // do not append CR to cell
        else cell.append(buf, bufMark, bufLen - bufMark);
        bufLen = reader.read(buf, 0, buf.length);
        bufMark = 0;
        // source is finished
        if (bufLen < 0) {
          cell = null;
          bufPos = -1; // say end of file to next call
          break;
        }
        bufPos = 0;
      }
      final char c = buf[bufPos++];
      // escaping char ? what to do
      if (lastChar == CR) {
        if(c != LF) { // old mac line
          bufPos--;
          break;
        }
        else if ((bufPos - bufMark) > 1) crlf = 1;
      }
      lastChar = c;
      if (c == LF) break;
      if (c == CR) continue;
      // cell separator
      if (c == sep) {
        if (cell != null) cell.append(buf, bufMark, bufPos - bufMark - 1);
        bufMark = bufPos;
        cell = row.next();
      }
    }
    // append pending chars to current cell
    if (cell != null) cell.append(buf, bufMark, bufPos - bufMark - 1 - crlf);
    this.bufPos = bufPos;
    line++;
    return true;
  }
  public class Row
  {
    /** Predefined number of cells to populate */
    private final Chain[] cells;
    /** Number of columns */
    private final int cols;
    /** Internal pointer in cells */
    int pointer;
    /** constructor */
    public Row(int cols)
    {
      cells = new Chain[cols];
      for (int i = cols - 1; i >= 0; i--) {
        cells[i] = new Chain();
      }
      this.cols = cols;
    }
    /**
     * Reset all cells
     * @return
     */
    public Row reset()
    {
      this.pointer = 0;
      for (int i = cols - 1; i >= 0; i--) {
        cells[i].reset();
      }
      return this;
    }
    /**
     * Give next cell or null if no more
     */
    public Chain get(int col)
    {
      return cells[col];
    }
    /**
     * Give next cell or null if no more
     */
    public Chain next()
    {
      if (pointer >= cols) return null;
      return cells[pointer++];
    }
    @Override
    public String toString()
    {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      sb.append('|');
      for (int i = 0; i < cols; i++) {
        if (first) first = false;
        else sb.append("|\t|");
        sb.append(cells[i]);
      }
      sb.append('|');
      return sb.toString();
    }

  }

  public static void main(String[] args) throws IOException, ParseException, URISyntaxException
  {
    Reader reader;
    int i;
    long time;
    
    
    for (int loop = 0; loop < 10; loop++) {
      
      
      i = -1;
      HashMap<CharAtt, LexEntry> dic1 = new HashMap<CharAtt, LexEntry>();
      // nio is not faster
      // Path path = Paths.get(Tag.class.getResource("word.csv").toURI());
      // reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
      reader = new InputStreamReader(Tag.class.getResourceAsStream("word.csv"));
      CsvReader csv = new CsvReader(reader, 4);
      time = System.nanoTime();
      while (csv.readRow()) {
        i++;
        // dic1.put(new CharAtt(csv.row().get(0)), new LexEntry(csv.row().get(1), csv.row().get(2)));
      }
      System.out.println("csv: " + ((System.nanoTime() - time) / 1000000) + " ms line="+i);
      
    }
    
  }
}
