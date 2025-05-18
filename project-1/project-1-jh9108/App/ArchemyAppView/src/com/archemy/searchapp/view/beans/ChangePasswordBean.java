package com.archemy.searchapp.view.beans;


import com.blogspot.ramannanda.adf.utils.adffacesutils.ADFUtils;
import com.blogspot.ramannanda.adf.utils.adffacesutils.JSFUtils;

import java.io.IOException;

import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.context.FacesContext;

import javax.servlet.http.HttpSession;

import oracle.adf.share.ADFContext;
import oracle.adf.view.rich.component.rich.input.RichInputText;
import oracle.adf.view.rich.component.rich.layout.RichPanelGroupLayout;
import oracle.adf.view.rich.context.AdfFacesContext;

import oracle.binding.OperationBinding;

import org.apache.directory.fortress.core.model.Session;


public class ChangePasswordBean {
  private RichInputText oldPassword;
     private RichInputText newPassword;
     private RichInputText confNewPassword;
     private final ResourceBundle rb=ResourceBundle.getBundle("com.archemy.searchapp.view.ArchemyAppViewBundle");
     private RichPanelGroupLayout containerBinding;

     public ChangePasswordBean() {
     super();
     }

     public void setOldPassword(RichInputText oldPassword) {
         this.oldPassword = oldPassword;
     }

     public RichInputText getOldPassword() {
         return oldPassword;
     }

     public void setNewPassword(RichInputText newPassword) {
         this.newPassword = newPassword;
     }

     public RichInputText getNewPassword() {
         return newPassword;
     }

     public void setConfNewPassword(RichInputText confNewPassword) {
         this.confNewPassword = confNewPassword;
     }

     public RichInputText getConfNewPassword() {
         return confNewPassword;
     }

     /**
      * This method is used to change the password for the user
      * @return
      */
     public String changePassword(){
             String newPasswordValue=(String)newPassword.getValue();
             String confirmNewPasswordValue=(String)confNewPassword.getValue();
             if(!newPasswordValue.equals(confirmNewPasswordValue)){
                  JSFUtils.addFacesErrorMessage(rb.getString("ERR_VAL_PASSWORD_DO_NOT_MATCH"));
                  return null;
                 }
             OperationBinding opBinding=ADFUtils.findOperation("changePasswordForUser");
             Map paramsMap=opBinding.getParamsMap();
             Map sessionMap=ADFContext.getCurrent().getSessionScope();
             Session rbacSession=(Session)sessionMap.get("RBACSESSION");
             if(rbacSession!=null)
             {
             String userId=rbacSession.getUserId(); 
             paramsMap.put("userId",userId);
             paramsMap.put("password",oldPassword.getValue());
             paramsMap.put("newPassword",newPasswordValue);
             opBinding.execute();
             
             if(opBinding.getErrors().isEmpty()){
                     JSFUtils.addFacesInformationMessage(rb.getString("PASSWORD_HAS_BEEN_CHANGED"));
                     return "success";
                 }
             }
           
             JSFUtils.resetAttributes(getContainerBinding(), AdfFacesContext.getCurrentInstance());
          return null;  
         }

     public void setContainerBinding(RichPanelGroupLayout containerBinding) {
         this.containerBinding = containerBinding;
     }

     public RichPanelGroupLayout getContainerBinding() {
         return containerBinding;
     }

     public String changePasswordForced() {
         String newPasswordValue=(String)newPassword.getValue();
         String confirmNewPasswordValue=(String)confNewPassword.getValue();
         if(!newPasswordValue.equals(confirmNewPasswordValue)){
              JSFUtils.addFacesErrorMessage(rb.getString("ERR_VAL_PASSWORD_DO_NOT_MATCH"));
              return null;
             }
         OperationBinding opBinding=ADFUtils.findOperation("changePasswordForUser");
         Map paramsMap=opBinding.getParamsMap();
         Map sessionMap=ADFContext.getCurrent().getSessionScope();
         Session rbacSession=(Session)sessionMap.get("RBACSESSION");
         String userId;
         if(rbacSession!=null)
         {
         userId=rbacSession.getUserId(); 
         }
         else {
                 userId=(String)sessionMap.get("userIdTemp"); 
         }
                 paramsMap.put("userId",userId);
                 paramsMap.put("password",oldPassword.getValue());
                 paramsMap.put("newPassword",newPasswordValue);
                 opBinding.execute();
                 
                 if(opBinding.getErrors().isEmpty()){
                         FacesContext context = FacesContext.getCurrentInstance();
                         HttpSession session=(HttpSession)context.getExternalContext().getSession(false);
                         String ctxRoot=rb.getString("CONTEXT_ROOT_PATH");
                         if(rbacSession==null)
                         {
                         session.invalidate();
                         try {
                         context.getExternalContext().redirect(ctxRoot+"/faces/login.jspx");
                         } catch (IOException e) {
                         e.printStackTrace();
                         }
                         }
                         else{
                                 try {
                                 context.getExternalContext().redirect(ctxRoot+"/faces/secured/Home.jspx");
                                 } catch (IOException e) {
                                 e.printStackTrace();
                                 }
                             }
                         return "";
                     }
          return null;
     }
}
