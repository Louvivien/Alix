package alix.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Efficient Object to handle a sliding window of mutable String (Chain), Works
 * like a circular array that you can roll on a token line.
 * 
 * @author glorieux-f
 *
 * @param <T>
 */
public class ChainRoller extends Roller implements Iterable<Chain>
{
    /** Data of the sliding window */
    private final Chain[] data;

    /**
     * Constructor, init data
     */
    public ChainRoller(final int left, final int right)
    {
        super(left, right);
        data = new Chain[size];
        // Arrays.fill will repeat a reference to the same object but do not create it
        for (int i = 0; i < size; i++)
            data[i] = new Chain();
    }

    /**
     * Get a value by index, positive or negative, relative to center
     * 
     * @param pos
     * @return
     */
    public String get(final int pos)
    {
        return data[pointer(pos)].toString();
    }

    /**
     * Get a pointer on the chain at desired position. Be careful, the chain is
     * mutable
     * 
     * @param pos
     * @return
     */
    public Chain access(final int pos)
    {
        return data[pointer(pos)];
    }

    /**
     * Modify a value by an index
     * 
     * @param pos
     *            Position in the slider
     * @param chain
     *            A string value to modify the position
     * @return
     */
    public Chain set(final int pos, final String term)
    {
        return data[pointer(pos)].copy(term);
    }

    /**
     * Modify a value by an index
     * 
     * @param pos
     *            Position in the slider
     * @param chain
     *            A string value to modify the position
     * @return
     */
    public Chain set(final int pos, final Chain chain)
    {
        return data[pointer(pos)].copy(chain);
    }

    /**
     * Move index to the next position and return a pointer on the Chain
     * 
     * @return the new current chain
     */
    public Chain next()
    {
        data[pointer(left)].reset(); // clear the last chain to find it as first one
        center = pointer(+1);
        return data[center];
    }

    /**
     * Move index to the prev position and return a pointer on the Chain
     */
    public Chain prev()
    {
        center = pointer(-1);
        return data[center];
    }

    /**
     * Add a value by the end
     */
    public void push(final String value)
    {
        // Chain ret = data[ pointer( -left ) ];
        center = pointer(+1);
        data[pointer(right)].copy(value);
    }

    /**
     * Add a value by the end
     */
    public void push(final Chain value)
    {
        // Chain ret = data[ pointer( -left ) ];
        center = pointer(+1);
        data[pointer(right)].copy(value);
    }

    /**
     * A private class that implements iteration.
     * 
     * @author glorieux-f
     */
    class TermIterator implements Iterator<Chain>
    {
        int current = left; // the current element we are looking at

        /**
         * If cursor is less than size, return OK.
         */
        @Override
        public boolean hasNext()
        {
            if (current <= right) return true;
            else return false;
        }

        /**
         * Return current element
         */
        @Override
        public Chain next()
        {
            if (!hasNext()) throw new NoSuchElementException();
            return data[pointer(current++)];
        }
    }

    @Override
    public Iterator<Chain> iterator()
    {
        return new TermIterator();
    }

    /**
     * Show window content
     */
    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        for (int i = left; i <= right; i++) {
            if (i == 0) sb.append(" <");
            sb.append(get(i));
            if (i == 0) sb.append("> ");
            else if (i == right) ;
            else if (i == -1) ;
            else sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Test the Class
     * 
     * @param args
     */
    public static void main(String args[])
    {
        String text = "Son amant emmène un jour O se promener dans un quartier où" + " ils ne vont jamais.";
        ChainRoller win = new ChainRoller(-2, 5);
        for (String token : text.split(" ")) {
            win.push(token);
            System.out.println(win);
        }
        for (Chain s : win) {
            System.out.println(s);
        }
    }
}
