package alix.fr.query;

import alix.fr.dic.Tag;
import alix.util.Occ;

public class TestOrthTag extends TestTerm
{
  int tag;

  public TestOrthTag(final String term, final int tag) {
    super(term);
    this.tag = tag;
  }

  @Override
  public boolean test(Occ occ)
  {
    if (!occ.tag().equals(tag))
      return false;
    return chain.glob(occ.orth());
  }

  @Override
  public String label()
  {
    return chain + ":" + Tag.label(tag);
  }

}
