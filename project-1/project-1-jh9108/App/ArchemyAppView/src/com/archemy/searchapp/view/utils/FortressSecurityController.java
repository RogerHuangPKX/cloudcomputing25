package com.archemy.searchapp.view.utils;


import com.archemy.catalog.security.util.FortressSecurityUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import javax.servlet.http.HttpSession;

import oracle.adf.share.ADFContext;
import oracle.adf.share.logging.ADFLogger;

import org.apache.directory.fortress.core.AccessMgr;
import org.apache.directory.fortress.core.DelAccessMgr;
import org.apache.directory.fortress.core.SecurityException;
import org.apache.directory.fortress.core.model.Permission;
import org.apache.directory.fortress.core.model.Session;
import org.apache.directory.fortress.core.model.UserAdminRole;
import org.apache.directory.fortress.core.model.UserRole;


public class FortressSecurityController {
  public static final ADFLogger FortressSecurityControllerLogger =
    ADFLogger.createADFLogger(FortressSecurityController.class);

  public FortressSecurityController() {
    super();
  }

  /**
   * To check whether user is in role
   * @return
   */
  public static boolean isUserInRole(String roleName, Session rbacSession) {
    boolean userInRole = false;
    List<UserRole> roles = rbacSession.getRoles();
    List<UserAdminRole> adminRoles = rbacSession.getAdminRoles();
    if (roles != null && !roles.isEmpty()) {
      Iterator it = roles.iterator();
      while (it.hasNext()) {
        UserRole role = (UserRole)it.next();
        if (role.getName().equals(roleName)) {
          return true;
        }
      }
    }
    if (adminRoles != null && !adminRoles.isEmpty()) {
      Iterator it = adminRoles.iterator();
      while (it.hasNext()) {
        UserAdminRole role = (UserAdminRole)it.next();
        if (role.getName().equals(roleName)) {
          return true;
        }
      }
    }
    return userInRole;
  }

  /**
   * This method returns users permissions in a map
   * @return
   */
  public static Map getPermissions(Session rbacSession) {
    String methodName = Thread.currentThread().getStackTrace()[1].getMethodName();

    Map permsMap = new HashMap();
    FortressSecurityUtil ops = new FortressSecurityUtil();
    AccessMgr mgr = ops.createAndGetAccessMgr();
    DelAccessMgr delMgr = ops.createAndGetDelAccessMgr();

    try {
      List<Permission> perms = delMgr.sessionPermissions(rbacSession);
      if (perms != null) {
        Iterator it = perms.iterator();
        while (it.hasNext()) {
          Permission permn = (Permission)it.next();
          String mapKey = permn.getObjName() + ":" + permn.getOpName() + ":Admin";
          permsMap.put(mapKey, true);
        }

      }
    } catch (SecurityException e) {
      FortressSecurityControllerLogger.fine("[" + methodName + "]" + "The user [" +
                                            rbacSession.getUserId() + "] does not have access", e);
    }

    try {
      List<Permission> perms = mgr.sessionPermissions(rbacSession);
      if (perms != null) {
        Iterator it = perms.iterator();
        while (it.hasNext()) {
          Permission permn = (Permission)it.next();
          String mapKey = permn.getObjName() + ":" + permn.getOpName() + ":Normal";
          permsMap.put(mapKey, true);
        }

      }
    } catch (SecurityException e) {
      FortressSecurityControllerLogger.fine("[" + methodName + "]" + "The user [" +
                                            rbacSession.getUserId() + "] does not have access", e);
    }

    return permsMap;
  }

  public static boolean checkPermission(String permissionString) {
    Map fortressPermMap =
      (Map)ADFContext.getCurrent().getSessionScope().get("FortressPermissionMap");

    if (fortressPermMap == null) {
      FacesContext context = FacesContext.getCurrentInstance();
      if (context != null) {
        HttpSession obj = (HttpSession)context.getExternalContext().getSession(false);
        HttpSession session = obj;
        fortressPermMap = getPermissions((Session)session.getAttribute("RBACSESSION"));
      }
    }
    if (fortressPermMap.get(permissionString) != null) {
      return (Boolean)fortressPermMap.get(permissionString);
    } else {
      return false;
    }

  }
}

