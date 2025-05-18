package com.archemy.searchapp.view.el.resolver;


import com.archemy.searchapp.view.utils.FortressSecurityController;
import com.archemy.searchapp.view.utils.PropertyEvaluator;

import com.sun.faces.util.Util;

import java.beans.FeatureDescriptor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ELResolver;

import javax.faces.context.FacesContext;

import javax.servlet.http.HttpSession;

import oracle.adf.share.logging.ADFLogger;

import org.apache.directory.fortress.core.model.Session;


public class FortressSecurityResolver extends ELResolver {
    public static final ADFLogger FortressRoleResolver = ADFLogger.createADFLogger(FortressSecurityResolver.class);
     
   
      public FortressSecurityResolver() {
        super();
       }

    @Override
    public Object getValue(ELContext elContext, Object base,
                           Object property) {
        String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();
        if(property!=null && property instanceof String && ((String)property).startsWith("Fortress")){
            elContext.setPropertyResolved(true);
            return new PropertyEvaluator((String)property);
        }
       else if(base instanceof PropertyEvaluator){
                String origProperty=((PropertyEvaluator)base).getProperty();
                FacesContext context = FacesContext.getCurrentInstance();
                if(context!=null){
                HttpSession obj=(HttpSession)  context.getExternalContext().getSession(false); 
                HttpSession session=obj;
                //this was already stored during login process
                if(session.getAttribute("RBACSESSION")!=null){
                    if(property!=null){
                    elContext.setPropertyResolved(true);
                    }
                    if(origProperty.equals("FortressUserInRole")){
                   return FortressSecurityController.isUserInRole((String)property,(Session)session.getAttribute("RBACSESSION"));
                    }
                    if(origProperty.equals("FortressAllowed")){
                            Map map=(Map)session.getAttribute("FortressPermissionMap");
                            if(map==null){
                                map=FortressSecurityController.getPermissions((Session)session.getAttribute("RBACSESSION"));
                                session.setAttribute("FortressPermissionMap", map);
                                }
                            return map.get(property);
                        }
                    
                }
                }
            }
        return null;
    }

    @Override
    public Class<?> getType(ELContext elContext,Object base,
                           Object property) {
        
        return Object.class;
    }

    @Override
    public void setValue(ELContext elContext, Object base,
                           Object property,    Object value) {
    }

    @Override
    public boolean isReadOnly(ELContext elContext, Object base,
                           Object property) {
        return false;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext elContext,
                                                             Object base) {
        if (base != null) return null;
        ArrayList<FeatureDescriptor> list = new ArrayList<FeatureDescriptor>(14);
        list.add(Util.getFeatureDescriptor(
                    "FortressUserInRole", 
                    "FortressUserInRole",
                    "Checks whether user is in role",
                    Boolean.FALSE, 
                    Boolean.FALSE, 
                    Boolean.TRUE, 
                    Boolean.class, 
                    Boolean.FALSE)
        );
        list.add(Util.getFeatureDescriptor(
                    "FortressAllowed", 
                    "FortressAllowed",
                    "Checks whether user has the required permission",
                    Boolean.FALSE, 
                    Boolean.FALSE, 
                    Boolean.TRUE, 
                    Boolean.class, 
                    Boolean.FALSE)
        );
        return list.iterator();
        
       
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext elContext, Object object) {
        return null;
    }
}
