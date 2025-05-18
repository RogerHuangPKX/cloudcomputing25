package com.archemy.searchapp.model.queries;

import java.util.HashMap;
import java.util.Map;

import oracle.jbo.server.ViewObjectImpl;

import org.codehaus.groovy.runtime.InvokerHelper;


public class CustomViewObjectImpl extends ViewObjectImpl {
  public CustomViewObjectImpl() {
    super();
  }
  private class AgrFuncHelper extends HashMap
   {
     private String funcName;

     public AgrFuncHelper(String funcName) 
     {
       super();
       this.funcName = funcName;  
     }


     public Object get(Object key) 
     {
       //Invoke private method
       //of our DefaultRowSet (sum,count,avg,min,max)
       //key is argument expression for the aggr funcion being called
       //sum("Salary")

       return InvokerHelper.invokeMethod(getDefaultRowSet(), funcName, key);
     }

   }
  
  public Map getMax(){
    return new AgrFuncHelper("max");
    }
  
}
