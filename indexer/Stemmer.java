

public class Stemmer {
    private char[] b; // to store the word being stemmed
    private int i,   // tracks the position of the current character in b
            i_end, // marks the end of the stemmed word
            j, k;   // i used them as pointer in the stemming process
    private static final int INC = 50;   // the increment size for dynamic resizing the buffer
  //   unit of size whereby b is increased
    public Stemmer()
    {  b = new char[INC];
        i = 0;
        i_end = 0;
    }

    /*
     * Add a character to the word being stemmed.  When you are finished
     * adding characters, you can call stem(void) to stem the word.
     */

    public void add(char ch)
    {  if (i == b.length)
    {  char[] new_b = new char[i+INC];
        System.arraycopy(b, 0, new_b, 0, i);
        b = new_b;
    }
        b[i++] = ch;
    }


    /* Adds wLen characters to the word being stemmed contained in a portion
     * of a char[] array. This is like repeated calls of add(char ch), but
     * faster.
     */

    public void add(char[] w, int wLen)
    {  if (i+wLen >= b.length)
    {  char[] new_b = new char[i+wLen+INC];
        System.arraycopy(b, 0, new_b, 0, i);
        b = new_b;
    }
        for (int c = 0; c < wLen; c++) b[i++] = w[c];
    }

    /**
     * After a word has been stemmed, it can be retrieved by toString(),
     * or a reference to the internal buffer can be retrieved by getResultBuffer
     * and getResultLength (which is generally more efficient.)
     */
    @Override
    public String toString() { return new String(b,0,i_end); }

    /**
     * Returns the length of the word resulting from the stemming process.
     */
    public int getResultLength() { return i_end; }

    /**
     * Returns a reference to a character buffer containing the results of
     * the stemming process.  You also need to consult getResultLength()
     * to determine the length of the result.
     */
    public char[] getResultBuffer() { return b; }

    /* cons(i) is true <=> b[i] is a consonant. */

    private boolean cons(int i)
    {  //return switch (b[i]) {
//	        case 'a', 'e', 'i', 'o', 'u' -> false;
//	        case 'y' -> (i==0) ? true : !cons(i-1);
//	        default -> true;
//	    };

        if (b[i] == 'a')
            return false;
        else if (b[i] == 'e')
            return false;
        else if (b[i] == 'i')
            return false;
        else if (b[i] == 'o')
            return false;
        else if (b[i] == 'u')
            return false;



        else if (b[i] == 'y' && i==0)
            return true;
        else if (b[i] == 'y' && i!=0)
            return !cons(i-1);
        else
            return true;
    }

	   /* m() measures the number of consonant sequences between 0 and j. if c is
	      a consonant sequence and v a vowel sequence, and <..> indicates arbitrary
	      presence,

	         <c><v>       gives 0
	         <c>vc<v>     gives 1
	         <c>vcvc<v>   gives 2
	         <c>vcvcvc<v> gives 3
	         ....
	   */

    private  int m()
    {  int n = 0;
        int i = 0;
        while(true)
        {  if (i > j) return n;
            if (! cons(i)) break; i++;
        }
        i++;
        while(true)
        {  while(true)
        {  if (i > j) return n;
            if (cons(i)) break;
            i++;
        }
            i++;
            n++;
            while(true)
            {  if (i > j) return n;
                if (! cons(i)) break;
                i++;
            }
            i++;
        }
    }

    /* vowelinstem() is true <=> 0,...j contains a vowel */

    private boolean vowelinstem()
    {  int i; for (i = 0; i <= j; i++) if (! cons(i)) return true;
        return false;
    }

    /* doublec(j) is true <=> j,(j-1) contain a double consonant. */

    private boolean doublec(int j)
    {  if (j < 1) return false;
        if (b[j] != b[j-1]) return false;
        return cons(j);
    }

	   /* cvc(i) is true <=> i-2,i-1,i has the form consonant - vowel - consonant
	      and also if the second c is not w,x or y. this is used when trying to
	      restore an e at the end of a short word. e.g.

	         cav(e), lov(e), hop(e), crim(e), but
	         snow, box, tray.

	   */

    private boolean cvc(int i)
    {  if (i < 2 || !cons(i) || cons(i-1) || !cons(i-2)) return false;
        {  int ch = b[i];
            if (ch == 'w' || ch == 'x' || ch == 'y') return false;
        }
        return true;
    }

    private boolean ends(String s)
    {  int l = s.length();
        int o = k-l+1;
        if (o < 0) return false;
        for (int i = 0; i < l; i++) if (b[o+i] != s.charAt(i)) return false;
        j = k-l;
        return true;
    }

	   /* setto(s) sets (j+1),...k to the characters in the string s, readjusting
	      k. */

    private void setto(String s)
    {  int l = s.length();
        int o = j+1;
        for (int i = 0; i < l; i++) b[o+i] = s.charAt(i);
        k = j+l;
    }

    /* r(s) is used further down. */

    private  void r(String s) { if (m() > 0) setto(s); }

	   /* step1() gets rid of plurals and -ed or -ing. e.g.

	          caresses  ->  caress
	          ponies    ->  poni
	          ties      ->  ti
	          caress    ->  caress
	          cats      ->  cat

	          feed      ->  feed
	          agreed    ->  agree
	          disabled  ->  disable

	          matting   ->  mat
	          mating    ->  mate
	          meeting   ->  meet
	          milling   ->  mill
	          messing   ->  mess

	          meetings  ->  meet

	   */

    private void step1()
    {  if (b[k] == 's')
    {  if (ends("sses")) k -= 2; else
    if (ends("ies")) setto("i"); else
    if (b[k-1] != 's') k--;
    }
        if (ends("eed")) { if (m() > 0) k--; } else
        if ((ends("ed") || ends("ing")) && vowelinstem())
        {  k = j;
            if (ends("at")) setto("ate"); else
            if (ends("bl")) setto("ble"); else
            if (ends("iz")) setto("ize"); else
            if (doublec(k))
            {  k--;
                {  int ch = b[k];
                    if (ch == 'l' || ch == 's' || ch == 'z') k++;
                }
            }
            else if (m() == 1 && cvc(k)) setto("e");
        }
    }

    /* step2() turns terminal y to i when there is another vowel in the stem. */

    private  void step2() { if (ends("y") && vowelinstem()) b[k] = 'i'; }

	   /* step3() maps double suffices to single ones. so -ization ( = -ize plus
	      -ation) maps to -ize etc. note that the string before the suffix must give
	      m() > 0. */

    private  void step3() { if (k == 0) return; /* For Bug 1 */

        if (b[k-1] == 'a')
        {
            if (ends("ational")) { r("ate");  }
            if (ends("tional")) { r("tion");  }
        }
        else if (b[k-1] == 'c') {
            if (ends("enci")) { r("ence");  }
            if (ends("anci")) { r("ance"); }
        }
        else if (b[k-1] == 'e') {
            if (ends("izer")) { r("ize");  }
        }
        else if (b[k-1] == 'l') {
            if (ends("bli")) { r("ble"); }
            if (ends("alli")) { r("al");  }
            if (ends("entli")) { r("ent");  }
            if (ends("eli")) { r("e");  }
            if (ends("ousli")) { r("ous");  }
        }
        else if (b[k-1] == 'o') {
            if (ends("ization")) { r("ize");  }
            if (ends("ation")) { r("ate");  }
            if (ends("ator")) { r("ate");  }
        }
        else if (b[k-1] == 's') {
            if (ends("alism")) { r("al");  }
            if (ends("iveness")) { r("ive");  }
            if (ends("fulness")) { r("ful");  }
            if (ends("ousness")) { r("ous");  }
        }
        else if (b[k-1] == 't') {
            if (ends("aliti")) { r("al");  }
            if (ends("iviti")) { r("ive");  }
            if (ends("biliti")) { r("ble");  }
        }
        else if (b[k-1] == 'g') {
            if (ends("logi")) {
                r("log");
            }
        }
    }

    /* step4() deals with -ic-, -full, -ness etc. similar strategy to step3. */

    private  void step4() {

        if (b[k] == 'e') {
            if (ends("icate")) { r("ic");  }
            if (ends("ative")) { r("");  }
            if (ends("alize")) { r("al");}
        }
        else if (b[k] == 'i') {
            if (ends("iciti")) { r("ic");  }
        }
        else if (b[k] == 'l') {
            if (ends("ical")) { r("ic");  }
            if (ends("ful")) { r("");  }
        }
        else if (b[k] == 's') {
            if (ends("ness")) { r("");  }
        }
    }

    /* step5() takes off -ant, -ence etc., in context <c>vcvc<v>. */

    private  void step5()
    {
        boolean found = false;
        if (k == 0) return; /* for Bug 1 */
        if (b[k-1] == 'a')  {
            if (ends("al")) found=true; return;
        }
        else if (b[k-1] == 'c' && !found)  {
            if (ends("ance")) found=true;
            if (ends("ence")) found=true;
        }
        else if (b[k-1] == 'e'&& !found) {
            if (ends("er")) found=true;
        }
        else if (b[k-1] == 'i'&& !found) {
            if (ends("ic")) found=true;
        }
        else if (b[k-1] == 'l'&& !found) {
            if (ends("able")) found=true;
            if (ends("ible")) found=true;
        }
        else if (b[k-1] == 'n'&& !found) {
            if (ends("ant")) found=true;
            if (ends("ement")) found=true;
            if (ends("ment")) found=true;
            /* element etc. not stripped before the m */
            if (ends("ent")) found=true;
        }
        else if (b[k-1] == 'o'&& !found) {
            if (ends("ion") && j >= 0 && (b[j] == 's' || b[j] == 't')) found=true;
            /* j >= 0 fixes Bug 2 */
            if (ends("ou")) found=true;
        }

        else if (b[k-1] == 's'&& !found) {
            if (ends("ism")) found=true;
        }
        else if (b[k-1] == 't'&& !found) {
            if (ends("ate")) found=true;
            if (ends("iti")) found=true;
        }
        else if (b[k-1] == 'u'&& !found) {
            if (ends("ous")) found=true;
        }
        else if (b[k-1] == 'v'&& !found) {
            if (ends("ive")) found=true;
        }
        else if (b[k-1] == 'z'&& !found) {
            if (ends("ize")) found=true;
        }
        if ( !found) {
            return;
        }

        /* takes care of -ous */
        if (m() > 1) k = j;
    }

    /* step6() removes a final -e if m() > 1. */

    private void step6()
    {  j = k;
        if (b[k] == 'e')
        {  int a = m();
            if (a > 1 || a == 1 && !cvc(k-1)) k--;
        }
        if (b[k] == 'l' && doublec(k) && m() > 1) k--;
    }

    /** Stem the word placed into the Stemmer buffer through calls to add().
     * Returns true if the stemming process resulted in a word different
     * from the input.  You can retrieve the result with
     * getResultLength()/getResultBuffer() or toString().
     */
    public void stem()
    {  k = i - 1;
        if (k > 1) { step1(); //step2();
            step3(); step4(); step5(); step6(); }
        i_end = k+1; i = 0;
    }

}
