package com.archemy.searchapp.view.http.filter;

import java.io.IOException;

import java.util.ResourceBundle;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.directory.fortress.core.model.Session;


public class SecurityFilter implements Filter {

  private static final ResourceBundle rb =
    ResourceBundle.getBundle("com.archemy.searchapp.view.ArchemyAppViewBundle");

  @Override
  public void init(FilterConfig filterConfig) {
  }

  public boolean checkLogin(ServletRequest request) {
    boolean login = false;
    HttpSession session = ((HttpServletRequest)request).getSession(false);
    if (session != null) {
      Session rbacSession = (Session)session.getAttribute("RBACSESSION");
      if (rbacSession != null) {
        login = true;
      }
    }
    return login;
  }

  public boolean checkReset(ServletRequest request) {
    Boolean isReset = false;
    HttpSession session = ((HttpServletRequest)request).getSession(false);
    if (session != null) {
      Session rbacSession = (Session)session.getAttribute("RBACSESSION");
      if (rbacSession != null) {
        isReset = rbacSession.getUser().isReset();
      }
    }
    return isReset;
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                       FilterChain filterChain) throws IOException, ServletException {
    String ctxRoot = rb.getString("CONTEXT_ROOT_PATH");
    boolean loggedIn = checkLogin(servletRequest);
    if (loggedIn) {
      filterChain.doFilter(servletRequest, servletResponse);
    } else {
      ((HttpServletResponse)servletResponse).sendRedirect(ctxRoot + "/faces/login.jspx");
    }
  }

  @Override
  public void destroy() {
  }
}
