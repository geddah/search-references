package com.equifax.search.main;

import java.util.ArrayList;

public class ReferencesArrayList<E>
  extends ArrayList<E>
{
  private static final long serialVersionUID = -6472151692002574804L;
  
  public ReferencesArrayList superStringsInList(String entry)
  {
    ReferencesArrayList arrayList = null;
    if (contains(entry))
    {
      arrayList = new ReferencesArrayList();
      for (E element : this) {
        if (((element instanceof String)) && 
          (((String)element).contains(entry)) && (!element.equals(entry))) {
          arrayList.add(element);
        }
      }
    }
    return arrayList;
  }
}
