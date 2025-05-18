package com.archemy.searchapp.view.beans;


import com.archemy.catalog.security.util.FortressSecurityUtil;

import com.blogspot.ramannanda.adf.utils.adffacesutils.ADFUtils;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.context.FacesContext;

import javax.servlet.http.HttpSession;

import oracle.adf.share.ADFContext;
import oracle.adf.share.logging.ADFLogger;
import oracle.adf.view.rich.component.rich.RichPopup;
import oracle.adf.view.rich.component.rich.input.RichInputText;

import oracle.binding.OperationBinding;

import org.apache.directory.fortress.core.AccessMgr;
import org.apache.directory.fortress.core.FinderException;
import org.apache.directory.fortress.core.GlobalErrIds;
import org.apache.directory.fortress.core.PasswordException;
import org.apache.directory.fortress.core.SecurityException;
import org.apache.directory.fortress.core.ValidationException;
import org.apache.directory.fortress.core.impl.GlobalPwMsgIds;
import org.apache.directory.fortress.core.model.Session;
import org.apache.directory.fortress.core.model.User;
import org.apache.directory.fortress.core.model.Warning;
import org.apache.myfaces.trinidad.render.ExtendedRenderKitService;
import org.apache.myfaces.trinidad.util.Service;


public class LoginBean {
  private RichInputText userName;
  private RichInputText password;
  private RichInputText licenseKey;
  private static final ResourceBundle rb =
    ResourceBundle.getBundle("com.archemy.searchapp.view.ArchemyAppViewBundle");
  public static final ADFLogger loginLogger = ADFLogger.createADFLogger(LoginBean.class);
  private RichPopup popUp;


  /**
   * To logout a user
   * @return
   */
  public String doLogout() {
    FacesContext context = FacesContext.getCurrentInstance();

    HttpSession session = (HttpSession)context.getExternalContext().getSession(false);
    String ctxRoot = rb.getString("CONTEXT_ROOT_PATH");
    session.invalidate();
    try {
      context.getExternalContext().redirect(ctxRoot + "/faces/login.jspx");
    } catch (IOException e) {
      loginLogger.info("Error occured while redirection", e);
    }
    return null;
  }

  public String doLogin() {
    String un = (String)userName.getValue();
    char[] pw = ((String)password.getValue()).toCharArray();
    FortressSecurityUtil ops = new FortressSecurityUtil();
    AccessMgr acMgr = ops.createAndGetAccessMgr();
    Map pfsScope = ADFContext.getCurrent().getPageFlowScope();
    FacesContext context = FacesContext.getCurrentInstance();
    HttpSession session = (HttpSession)context.getExternalContext().getSession(true);
    //check whether RBACSESSION already exists if so redirect the user to the home page
    if (session.getAttribute("RBACSESSION") != null) {
      return "success";
    }
    User user = new User();
    user.setUserId(un);
    user.setPassword(pw);
    try {
      Session rbacSession = acMgr.createSession(user, false);
      if (rbacSession != null) {
        //only for normal users should customer information form be there
        if(rbacSession.getAdminRoles().isEmpty()){
        OperationBinding opBinding=ADFUtils.findOperation("addCustomerRowIfNotExists"); 
        opBinding.getParamsMap().put("custId", un);
        opBinding.execute();
        //also check the license key
        String userLicenseKey=rbacSession.getUser().getProperty("licensekey");
        if(userLicenseKey!=null&&userLicenseKey.equals((String)licenseKey.getValue())){
          session.setAttribute("licenseValid", true);
          }
        }
        else{
            session.setAttribute("licenseValid", true);
          }
        
        //check for warnings if they contain password expiration warning warn him of the same
        //and provide option to change the password
        List<Warning> warnings = rbacSession.getWarnings();
        if (warnings != null && !warnings.isEmpty()) {
          Iterator<Warning> it = warnings.iterator();
          while (it.hasNext()) {
            Warning warning = it.next();
            if (warning.getId() == GlobalPwMsgIds.PASSWORD_EXPIRATION_WARNING) {
              pfsScope.put("errorMessage", warning.getMsg());
              session.setAttribute("RBACSESSION", rbacSession);
              ExtendedRenderKitService erks =
                Service.getRenderKitService(context, ExtendedRenderKitService.class);
              //show popup
              erks.addScript(context,
                             "AdfPage.PAGE.findComponent('" + popUp.getClientId() + "').show();");
              pfsScope.put("expireWarning", Boolean.TRUE);
              return "";
            }
          }
        }
        session.setAttribute("RBACSESSION", rbacSession);
        session.setAttribute("userId", un);
        context.responseComplete();
        return "success";
      }

    } catch (PasswordException e) {
      //this means the user's password was reset by admin and hence redirect user to change his password afterward you can log him out
      if (e.getErrorId() == GlobalErrIds.USER_PW_RESET) {
        if (session != null) {
          //temporary user id and user's pw policy which can be shown on the change password page
          session.setAttribute("userIdTemp", un);
          List<User> users = new ArrayList();
          users = ops.searchUsers(un);
          String pwPol = users.get(0).getPwPolicy();
          session.setAttribute("pwPol", pwPol);
          context.responseComplete();
          return "changePassword";
        }
      } else {
        loginLogger.fine("Login Failed due to authentication failure" + e.getMessage(), e);
        pfsScope.put("errorMessage",
                     "Login Failed due to authentication failure" + e.getMessage());
      }

    } catch (ValidationException e) {
      loginLogger.severe("User is not allowed access" + e.getMessage(), e);
      pfsScope.put("errorMessage", "User is not allowed access" + e.getMessage());
    } catch (FinderException e) {
      loginLogger.severe("User does not exist" + e.getMessage(), e);
      pfsScope.put("errorMessage", "User does not exist in the system");
    } catch (SecurityException e) {
      loginLogger.fine("Authentication failed due to" + e.getMessage(), e);
      pfsScope.put("errorMessage", "System Error Occured" + e.getMessage());
    }
    ExtendedRenderKitService erks =
      Service.getRenderKitService(context, ExtendedRenderKitService.class);
    //show popup
    erks.addScript(context, "AdfPage.PAGE.findComponent('" + popUp.getClientId() + "').show();");
    return "";
  }

  public void setUserName(RichInputText userName) {
    this.userName = userName;
  }

  public RichInputText getUserName() {
    return userName;
  }

  public void setPassword(RichInputText password) {
    this.password = password;
  }

  public RichInputText getPassword() {
    return password;
  }

  public void setPopUp(RichPopup popUp) {
    this.popUp = popUp;
  }

  public RichPopup getPopUp() {
    return popUp;
  }


  public void setLicenseKey(RichInputText licenseKey) {
    this.licenseKey = licenseKey;
  }

  public RichInputText getLicenseKey() {
    return licenseKey;
  }
}
