package com.archemy.searchapp.view.api;

import com.archemy.catalog.security.util.FortressSecurityUtil;

import com.archemy.searchapp.model.am.common.ArchemySearchAM;
import com.archemy.searchapp.model.queries.CustomerInfoVOImpl;
import com.archemy.searchapp.model.queries.CustomerInfoVORowImpl;
import com.archemy.searchapp.model.queries.KADDimensionsAreaTempVOImpl;
import com.archemy.searchapp.model.queries.KADDimensionsAreaTempVORowImpl;
import com.archemy.searchapp.model.queries.KadSearchTransVOImpl;
import com.archemy.searchapp.model.queries.KadSearchTransVORowImpl;
import com.archemy.searchapp.model.queries.KadTempVOImpl;
import com.archemy.searchapp.model.queries.KadTempVORowImpl;
import com.archemy.searchapp.model.queries.KadsVOImpl;
import com.archemy.searchapp.model.queries.KadsVORowImpl;

import java.io.IOException;
import java.io.PrintWriter;

import java.security.GeneralSecurityException;
import java.security.Key;

import java.security.NoSuchAlgorithmException;

import java.util.List;

import javax.servlet.*;
import javax.servlet.http.*;

import oracle.jbo.RowSetIterator;

import oracle.jbo.client.Configuration;

import oracle.jbo.server.ViewObjectImpl;

import oracle.jbo.server.ViewRowImpl;

import org.apache.directory.fortress.core.AccessMgr;
import org.apache.directory.fortress.core.FinderException;
import org.apache.directory.fortress.core.GlobalErrIds;
import org.apache.directory.fortress.core.PasswordException;
import org.apache.directory.fortress.core.SecurityException;
import org.apache.directory.fortress.core.ValidationException;
import org.apache.directory.fortress.core.model.Session;
import org.apache.directory.fortress.core.model.User;
import org.apache.directory.fortress.core.model.Warning;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

import java.util.ArrayList;
import java.util.Arrays;

import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import javax.xml.bind.DatatypeConverter;

import oracle.jbo.Row;

public class ArchAPI extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CONTENT_TYPE = "application/json; charset=UTF-8";

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /* Documentation for API Usage:

    Web.xml mapping to /archapi

     */

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(CONTENT_TYPE);
        PrintWriter out = response.getWriter();

        String JSON = "{";
        String user = null;

        ArchemySearchAM applicationModule = null;
        try {

            applicationModule =
                    (ArchemySearchAM)Configuration.createRootApplicationModule("com.archemy.searchapp.model.am.ArchemySearchAM",
                                                                               "ArchemySearchAMLocal");

            if (request.getParameter("commandLineApplication") != null &&
                Boolean.valueOf(request.getParameter("commandLineApplication"))) {
                out.println(commandLine(applicationModule, request));
                return;
            }

            int loginLevel = -1;
            if (request.getParameter("key") != null) {
                String privKey =
                    "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCPmv0zyS5JQr6INSYBdg+/qMm6mxE3AwtltwIGn3zt0y6r2e0RUH8+jKlli1yNLcT/epcxGxPigztCM8wSKWuzMbdEz1ED0pxaz/ba4hlmLlHiRAUG9nqb7mKVz1qlZZY11Rjkcsg5eA5gAdYEclMIr7eYenrQSEbaK4wSuyOLD1XH0f5a+UvudnuteksA8j5fkwsr7n2IvWIxaOzDlYAj5mQPALQYbQNFUllIUTJJkkQC/PHrnUOrTxdnFzgs+oQWtkEbENnE6ZKs08dk8jM4du/hmKL0sCI7DqAIwBT7UMCF8fT1gOsEC+hVbFTQbnTiRi2X1rhZQZqUbKn/hEbRAgMBAAECggEADgshIdRVw3JUgat46QGrrpmKCMarW07f6XWJLC6in/tcABBSv7O4jdxhoH2Ncnz8W+OYL4QvYKJmxCWemlQUpTSCcKc5i/8nrTXTNTqRM03qUg9G0pR+Dwuz9mSNv8j8dI0/Xu/epsgX18m2LT8k4Z+Ve8LWidHXo/RIQXitlCapBuDDoceD6WhI+iVoRGhLaSrzR+f04Sn58l735h1OuR6QPxIY9D1t+ox0adko/6m4NEsg7MkGDOXKwIaPRlEPojB115OnfCmZQIyoz0JcSUB5MdVIR33YTLiFryw01vQFUC7lBEqUV1cS7sYj35yUV2rylNkEkUk6B6fC+EXchQKBgQDWid5E3f70vEGBkMRzrQO9CFiocmkl8mDnS8heMsrSP6wfEjsm/Q4f70KBVMnzFzESfshNtvuiwYUdOi8+V9xFS9qJkO4hoHVCcbHqP6GEmrRJUEuP29zu9urwtINGhCoEcMm2DLOEM8wuQX5hDHe8wULADzmNoG8vJOM93uImFwKBgQCrW8IsgTTANidNBLBP9zMYluyOoPIoXnIV00Gju4X7fD4MrmNeR35GcAxKQiegoHniTqroCJ2FWuqLo136W1DlY1ws0C1hwE/T//BmKAZI70uZp0qDggg2zG9SgwQgLyNeZ/4OnCBW9X/8T5AO7+x+2YCcoraYMiO7kVZt1/tzVwKBgEmGHkKDwiili92Xe3wZQzq5bYjtDNQQaN1bv2NpDNFZOOe9G8CU4Q5YtPYV1NAWlp68DHF10G9K2w/VLPO0sKye/lo+7R1hHE6VIGAjRntneXnWps66jtDmlkW/122HRc8XyEk3uR4JkmQX1fP0jeSGZxXjIdpDrVb+0VIW3HIpAoGAECag4ZL4BtnT0HWNrKvO/BVVjIfs6xMjy5zSxfzpvu9R5d4V7Y/tffQXpHQhygj2E/d4MlCFkEkmbCzksbEjqcs4p9yjOmBm5cNsxCQnm346cOwMoOKDpa6VG4DPxbzLp51Dm9rpTWjsPDq/iDji4H3dmmXXsfaf2ZD0RXwi7hcCgYEAghxb2DbIGxXkgiB5rNgu041yN2ABzcORQDEqUXnmK8+1hPtPi6rvoGKeGwAEP6HLNRrJG9n9XnxRWV6MqbyJjKF5YSJVddrj4CXDYBAVvaRRHePAtA7cLrysBOGj0krxyDzZY6i64mB/jb5FxaVkABCKqhz9wR2hRLc3liH23gk=";
                String decoded = decrypt(request.getParameter("key"), loadPrivateKey(privKey));
                privKey = null;
                user = decoded.split("[|]")[0];
                String[] add = new String[] { "", "" };
                loginLevel = APILogin(applicationModule, user, decoded.split("[|]")[1].toCharArray(), add);
                if (loginLevel == -1)
                    user = null;

                JSON += add[0];
                if (!add[1].equals("")) {
                    JSON += add[1];
                }

                add = null;

            } else {
                JSON += "\"error\": {\"message\": \"No access key (key) specified.\", \"status\": 400},";
            }
            if (user != null && request.getParameter("q") != null) {

                int[] perms = callPerms(loginLevel);
                boolean found = false;
                for (int i = 0; i < perms.length; i++) {
                    if ((perms[i] + "").equals(request.getParameter("q").toString())) {
                        found = true;
                    }
                }

                if (!found) {
                    JSON +=
                            "\"error\": {\"message\": \"Invalid query or insufficient privileges.\", \"status\": 400},";
                } else {

                    /*
             * Query permissions:
             *
             * Normal User: 0, 1, 2, 3, 4
             *
             * Admin: 0, 1, 2, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
             */
                    switch (request.getParameter("q")) {
                    case "0":
                        JSON += searchKAD(applicationModule, request);
                        break;
                    case "1":
                        JSON += searchCatalog(applicationModule, request);
                        break;
                    case "2":
                        JSON += viewUsage(applicationModule, request);
                        break;
                    case "3":
                        JSON += registerKAD(applicationModule, request, user);
                        break;
                    case "4":
                        JSON += custProfile(applicationModule, request, user);
                        break;
                    case "5":
                        JSON += addKad(applicationModule, request);
                        break;
                    case "6":
                        JSON += deleteKad(applicationModule, request);
                        break;
                    case "7":
                        JSON += addDomain(applicationModule, request);
                        break;
                    case "8":
                        JSON += deleteDomain(applicationModule, request);
                        break;
                    case "9":
                        JSON += addDimension(applicationModule, request);
                        break;
                    case "10":
                        JSON += deleteDimension(applicationModule, request);
                        break;
                    case "11":
                        JSON += addArea(applicationModule, request);
                        break;
                    case "12":
                        JSON += deleteArea(applicationModule, request);
                        break;
                    case "13":
                        JSON += addBP(applicationModule, request);
                        break;
                    case "14":
                        JSON += deleteBP(applicationModule, request);
                        break;
                    case "15":
                        JSON += getCustInfo(applicationModule, request);
                        break;
                    default:
                        JSON += "\"error\": {\"message\": \"Invalid query type (q) specified.\", \"status\": 400},";
                    }
                }
            } else {
                if (user == null) {
                    JSON += "\"error\": {\"message\": \"Invalid access key (key) parameter.\", \"status\": 400},";
                } else {
                    JSON += "\"error\": {\"message\": \"No query type (q) specified.\", \"status\": 400},";
                }
            }

        } catch (GeneralSecurityException e) {
            JSON += "\"error\": {\"message\": \"" + e.getMessage() + "\", \"code\": 500},";
        } catch (Exception ex) {
            JSON += "\"error\": {\"message\": \"" + ex.getMessage() + "\", \"code\": 500},";
        } finally {
            if (applicationModule != null) {
                Configuration.releaseRootApplicationModule(applicationModule, false);
            }
        }

        if (JSON.substring(JSON.length() - 1).equals(",")) {
            JSON = JSON.substring(0, JSON.length() - 1);
        }

        JSON += "}";
        out.println(JSON);
        out.close();
    }

    /*
     * Archemy Normal User Methods
     */

    /*

    q = 0 | Search Knowledge Artifacts
        kn      KAD Name
        d       Domain
        kn2     KAD Name 2 for Between operations
        d2      DomainId 2 for Between operations
        kst     Kad Name Search Type    sRange: 0 - 13
        dst     Domain Search Type      sRange: 2 - 9 && 12 - 13

     */

    private String searchKAD(ArchemySearchAM applicationModule, HttpServletRequest request) {

        if (request.getParameter("h") != null) {
            return "\"data\": \"-Search KADs- Query Type (q): 0. This API endpoint takes up to six (6) parameters, all of which are optional. " +
                "\nThese parameters include:" + "\nPARAMETERS" + "\n - kn    OPTIONAL    -> Kad Name" +
                "\n - d     OPTIONAL    -> Domain" +
                "\n - kn2   OPTIONAL    -> Kad Name 2, used for \"between\" and \"not between\" operations on Kad Names" +
                "\n - d2  OPTIONAL    -> Domain 2, used for \"between\" and \"not between\" operations on Domains" +
                "\n - kst   OPTIONAL    -> Search type for Kad Name to be used to compare Kad Name to those in the database (i.e. equals, not equal to)" +
                "\n - dst   OPTIONAL    -> Search type for Domain to be used to compare Domain Id to those in the database (i.e. equals, not equal to)" +
                "\"";
        }

        String resDom = resolveDomain(request.getParameter("d"), applicationModule);
        String resDom2 = resolveDomain(request.getParameter("d2"), applicationModule);

        if (request.getParameter("d") != null && resDom == null)
            return "\"error\": {\"message\": \"Domain (d) could not be resolved to exist.\", \"staus\": 400},";

        if (request.getParameter("d2") != null && resDom2 == null && (request.getParameter("dst").equals("8") || request.getParameter("dst").equals("9")))
            return "\"error\": {\"message\": \"Domain 2 (d2) could not be resolved to exist.\", \"staus\": 400},";

        KadsVOImpl kads = (KadsVOImpl)applicationModule.findViewObject("KadsVO3");

        String whereClause = "";

        if (request.getParameter("kn") != null) {
            if (whereClause.length() != 0) {
                whereClause += " AND ";
            }

            if (request.getParameter("kst") != null) {
                try {
                    int type = Integer.parseInt(request.getParameter("kst").toString());
                    if (type < 0 || type > 13) {
                        return "\"error\": {\"message\": \"Kad Name search type (kst) must be in range 0 to 13.\", \"status\": 400},";
                    }
                } catch (NumberFormatException e) {
                    return "\"error\": {\"message\": \"Kad Name search type (kst) is not a valid integer.\", \"staus\": 400},";
                }
            }

            whereClause +=
                    "KadsEO.KAD_NAME" + makeWhere(request.getParameter("kst"), request.getParameter("kn"), request.getParameter("kn2"));
        }

        if (request.getParameter("d") != null) {
            if (whereClause.length() != 0) {
                whereClause += " AND ";
            }

            if (request.getParameter("dst") != null) {
                try {
                    int type = Integer.parseInt(request.getParameter("dst").toString());
                    if (type < 0 || type > 13 || (type > 9 && type < 12)) {
                        return "\"error\": {\"message\": \"Name domain type (dst) must be in range 0 to 9 or 12 to 13.\", \"status\": 400},";
                    }
                } catch (NumberFormatException e) {
                    return "\"error\": {\"message\": \"Domain search type (dst) is not a valid integer.\", \"status\": 400},";
                }
            }
            whereClause += "KadsEO.DOMAIN_ID" + makeWhere(request.getParameter("dst"), resDom, resDom2);
        }

        kads.addWhereClause(whereClause);
        kads.executeQuery();
        RowSetIterator rs = kads.createRowSetIterator(null);

        String data = "[";
        while (rs.hasNext()) {
            KadsVORowImpl res = (KadsVORowImpl)rs.next();
            String entry = "{";
            entry += "\"KadId\": \"" + res.getKadId() + "\", ";
            entry += "\"KadName\": \"" + res.getKadName() + "\", ";
            entry += "\"DomainId\": \"" + res.getDomainId() + "\"";
            entry += "}";
            data += entry;
            if (rs.hasNext()) {
                data += ",";
            }
        }
        data += "]";
        return "\"data\": " + data + ", \"status\": 200},";
    }

    /*

    q = 1 | Search or Add Catalog
        d       Domain                  REQUIRED
        bp      Business Problem
        nc      Number of Criterias     REQUIRED
        c1d     Criteria1 Dimension     REQUIRED
        c1aid   Criteria1 AreaId
        c1ca    Criteria1 Child Area
        c1w     Criteria1 Weight        REQUIRED *sum up to 100
        c1cl    Criteria1 Closeness

        Additional Options:
        -------------------
        cNd     CriteriaN Dimension     REQUIRED (per criteria)
        cNaid   CriteriaN AreaId
        cNca    CriteriaN Child Area
        cNw     CriteriaN Weight        REQUIRED (per criteria) *sum up to 100
        cNcl    CriteriaN Closeness

     */

    private String searchCatalog(ArchemySearchAM applicationModule, HttpServletRequest request) {

        if (request.getParameter("h") != null) {
            return "\"data\": \"-Search Catalog- Query Type (q): 1. This API endpoint takes up to as many parameters as provided to return an array of KADs and their scores according to the provided criteria. " +
                "\nThese parameters include:" + "\nPARAMETERS" + "\n - d     REQUIRED    -> Domain to be searched" +
                "\n - bp    OPTIONAL    -> Specific Business Problem" +
                "\n - nc    REQUIRED    -> Number of criteria being passed. Must be >= 1" +
                "\n - c1d   REQUIRED    -> The first criteria's dimension. This is always required because we always have at least one (1) criteria" +
                "\n - c1aid OPTIONAL    -> Area Id of the first criteria" +
                "\n - c1ca  OPTIONAL    -> Area Parent of the first criteria" +
                "\n - c1w   REQUIRED*   -> Weight to be given during ranking. *The total weights across all criteria must sum up to 100" +
                "\n - c1cl  OPTIONAL    -> Closeness of the first criteria" + "\n ADDITIONAL PARAMETERS" +
                "\n - cNd   REQUIRED*   -> Dimension Id of the Nth criteria. Dimension is required for each criteria" +
                "\n - cNaid OPTIONAL    -> Area Id of the Nth criteria" +
                "\n - cNca  OPTIONAL    -> Area Parent of the Nth criteria" +
                "\n - cNw   REQUIRED*   -> Weight of the Nth criteria. *Weight across all criteria must sum up to 100" +
                "\n - cNcl  OPTIONAL    -> Closeness of the Nth criteria" + "\"";
        }

        KADDimensionsAreaTempVOImpl kadSearchCriteriaVO =
            (KADDimensionsAreaTempVOImpl)applicationModule.findViewObject("KADDimensionsAreaTempVO1");

        if (request.getParameter("d") == null) {
            return "\"error\": {\"message\": \"Domain (d) is not specified.\", \"status\": 400},";
        }

        if (request.getParameter("nc") == null) {
            return "\"error\": {\"message\": \"Number of criteria (nc) is not specified.\", \"status\": 400},";
        }

        int crits;
        try {
            crits = Integer.parseInt(request.getParameter("nc").toString());
        } catch (NumberFormatException e) {
            return "\"error\": {\"message\": \"Number of criteria (nc) is not a valid integer.\", \"status\": 400},";
        }

        if (crits < 1) {
            return "\"error\": {\"message\": \"Number of criteria (nc) must be at least 1.\", \"status\": 400},";
        }

        int totalWeight = 0;
        for (int i = 0; i < crits; i++) {
            KADDimensionsAreaTempVORowImpl addRow = (KADDimensionsAreaTempVORowImpl)kadSearchCriteriaVO.createRow();

            int criteriaDimension;
            if (request.getParameter("c" + (i + 1) + "d") == null) {
                return "\"error\": {\"message\": \"Critera " + (i + 1) + " dimension (c" + (i + 1) +
                    "d) is not specified.\", \"status\": 400},";
            } else {
                try {
                    String resDim = resolveDimension(request.getParameter("c" + (i + 1) + "d"), applicationModule);
                    if (resDim == null)
                        return "\"error\": {\"message\": \"Critera " + (i + 1) + " dimension (c" + (i + 1) +
                            "d) cannot be resolved to a corresponding valid Integer Id of a Dimension.\", \"status\": 400},";
                    criteriaDimension = Integer.parseInt(resDim);
                    addRow.setDimensionId(criteriaDimension);
                } catch (NumberFormatException e) {
                    return "\"error\": {\"message\": \"Critera " + (i + 1) + " dimension (c" + (i + 1) +
                        "d) cannot be resolved to a corresponding valid Integer Id of a Dimension.\", \"status\": 400},";
                }
            }

            int criteriaAreaId;
            if (request.getParameter("c" + (i + 1) + "aid") != null) {
                try {
                    String resA = resolveArea(request.getParameter("c" + (i + 1) + "aid"), applicationModule);
                    if (resA == null)
                        return "\"error\": {\"message\": \"Critera " + (i + 1) + " dimension (c" + (i + 1) +
                            "aid) cannot be resolved to a corresponding valid Integer Id of an Area.\", \"status\": 400},";
                    criteriaAreaId = Integer.parseInt(resA);
                    addRow.setAreaId(criteriaAreaId);
                } catch (NumberFormatException e) {
                    return "\"error\": {\"message\": \"Critera " + (i + 1) + " area id (c" + (i + 1) +
                        "aid) could not be resolved to correspond to a valid Integer Id of an Area.\", \"status\": 400},";
                }
            }

            int criteriaCA;
            if (request.getParameter("c" + (i + 1) + "ca") != null) {
                try {
                    String resCA = resolveArea(request.getParameter("c" + (i + 1) + "ca"), applicationModule);
                    if (resCA == null)
                        return "\"error\": {\"message\": \"Critera " + (i + 1) + " dimension (c" + (i + 1) +
                            "ca) cannot be resolved to a corresponding valid Integer Id of an Area.\", \"status\": 400},";
                    criteriaCA = Integer.parseInt(resCA);
                    addRow.setAreaChildId(criteriaCA);
                } catch (NumberFormatException e) {
                    return "\"error\": {\"message\": \"Critera " + (i + 1) + " child area (c" + (i + 1) +
                        "ca) could not be resolved to a corresponding valid Integer Id for an Area.\", \"status\": 400},";
                }
            }

            int criteriaW;
            if (request.getParameter("c" + (i + 1) + "w") != null) {
                try {
                    criteriaW = Integer.parseInt(request.getParameter("c" + (i + 1) + "w").toString());
                    addRow.setWeight(criteriaW);
                    totalWeight += criteriaW;
                } catch (NumberFormatException e) {
                    return "\"error\": {\"message\": \"Critera " + (i + 1) + " weight (c" + (i + 1) +
                        "w) is not a valid integer.\", \"status\": 400},";
                }
            }

            int criteriaCL;
            if (request.getParameter("c" + (i + 1) + "cl") != null) {
                try {
                    criteriaCL = Integer.parseInt(request.getParameter("c" + (i + 1) + "cl").toString());
                    addRow.setCloseness(criteriaCL);
                } catch (NumberFormatException e) {
                    return "\"error\": {\"message\": \"Critera " + (i + 1) + " closeness (c" + (i + 1) +
                        "cl) is not a valid integer.\", \"status\": 400},";
                }
            }
            kadSearchCriteriaVO.insertRow(addRow);
        }

        if (totalWeight < 100) {
            return "\"error\": {\"message\": \"Total weight across all criteria must equal (=) 100.\", \"status\": 400},";
        }

        applicationModule.searchAndRankKad(null);
        KadSearchTransVOImpl vo = (KadSearchTransVOImpl)applicationModule.findViewObject("KadSearchTransVO1");
        vo.executeQuery();

        RowSetIterator rs = vo.createRowSetIterator(null);
        String data = "[";
        while (rs.hasNext()) {
            String entry = "{";

            KadSearchTransVORowImpl res = (KadSearchTransVORowImpl)rs.next();
            entry += "\"KadId\": \"" + res.getKadID() + "\", ";
            entry += "\"KadLink\": \"" + res.getKadLink() + "\", ";
            entry += "\"KadLinkPublic\": \"" + res.getKadLinkPublic() + "\", ";
            entry += "\"KadName\": \"" + res.getKadName() + "\", ";
            entry += "\"DomainName\": \"" + res.getDomainName() + "\", ";
            entry += "\"HitCounter\": \"" + res.getHitCounter() + "\", ";
            entry += "\"Score\": \"" + res.getScore() + "\"";

            entry += "}";
            data += entry;
            if (rs.hasNext()) {
                data += ",";
            }
        }
        data += "]";
        return "\"data\": " + data + ", \"status\": 200,";
    }

    /*

    q = 2 | View Usage Statistics
        cname   -   CustomerName                sRange: 0 - 13
        cname2  -   Second Between agument
        cnamet  -   CustomerName sType          Default: 0 sRange: 0 - 13
        ind     -   Industry                    
        ind2    -   Second Between agument
        indT    -   Industry sType              Default: 0 sRange: 0 - 13
        appe    -   ApplicabilityExtent         
        appe2   -   Second Between agument
        appet   -   ApplicabilityExtent sType   Default: 0 sRange: 2 - 9 && 12 - 13
        br      -   BenefitRating               
        br2     -   Second Between agument
        brt     -   BenefitRating sType         Default: 0 sRange: 0 - 13
        c       -   Comments                    
        c2      -   Second Between agument
        ct      -   Comments sType              Default: 0 sRange: 0 - 13
        ds      -   DeploymentStatus           
        ds2     -   Second Between agument
        dst     -   DeploymentStatus sType      Default: 0 sRange: 2 - 9 && 12 - 13
        k       -   Kad                      
        k2      -   Second Kad for Between agument
        kst     -   Kad sType                 Default: 0 sRange: 2 - 9 && 12 - 13
        mr      -   MaturityRating             
        mr2     -   Second Between agument
        mrt     -   MaturityRating sType        Default: 0 sRange: 0 - 13

     */

    private String viewUsage(ArchemySearchAM applicationModule, HttpServletRequest request) {

        if (request.getParameter("h") != null) {
            return "\"data\": \"-View Usage Statistics- Query Type (q): 2. This API endpoint takes up to twenty-four (24) parameters, all of which are optional. " +
                "\nThese parameters include:" + "\nPARAMETERS" +
                "\ncname    -   OPTIONAL    -> Filter search results by Customer Name" +
                "\ncname2   -   OPTIONAL    -> Second Customer Name used in \"between\" and \"not between\" searches" +
                "\ncnamet   -   OPTIONAL    -> Search Type for Customer Name (i.e. equals, not equals) can range from 0 - 13" +
                "\nind      -   OPTIONAL    -> Filter search results by Industry" +
                "\nind2     -   OPTIONAL    -> Second Industry used in \"between\" and \"not between\" searches" +
                "\nindt     -   OPTIONAL    -> Search Type for Industry (i.e. equals, not equals) can range from 0 - 13" +
                "\nappe     -   OPTIONAL    -> Filter search results by Applicability Extent" +
                "\nappe2    -   OPTIONAL    -> Second Applicability Extent used in \"between\" and \"not between\" searches" +
                "\nappet    -   OPTIONAL    -> Search Type for Applicability Extent (i.e. equals, not equals) can range from 0 - 13" +
                "\nbr       -   OPTIONAL    -> Filter search results by Benefit Rating" +
                "\nbr2      -   OPTIONAL    -> Second Benefit Rating used in \"between\" and \"not between\" searches" +
                "\nbrt      -   OPTIONAL    -> Search Type for Benefit Rating (i.e. equals, not equals) can range from 2 - 9 && 12 - 13" +
                "\nc        -   OPTIONAL    -> Filter search results by Comments" +
                "\nc2       -   OPTIONAL    -> Second Comment used in \"between\" and \"not between\" searches" +
                "\nct       -   OPTIONAL    -> Search Type for Comments (i.e. equals, not equals) can range from 0 - 13" +
                "\nds       -   OPTIONAL    -> Filter search results by Deployment Status" +
                "\nds2      -   OPTIONAL    -> Second Deployment Status used in \"between\" and \"not between\" searches" +
                "\ndst      -   OPTIONAL    -> Search Type for Deployment Status (i.e. equals, not equals) can range from 0 - 13" +
                "\nk        -   OPTIONAL    -> Filter search results by Kad" +
                "\nk        -   OPTIONAL    -> Second Kad used in \"between\" and \"not between\" searches" +
                "\nkst      -   OPTIONAL    -> Search Type for Kad (i.e. equals, not equals) can range from 2 - 9 && 12 - 13" +
                "\nmr       -   OPTIONAL    -> Filter search results by Maturity Rating" +
                "\nmr2      -   OPTIONAL    -> Second Maturity Rating used in \"between\" and \"not between\" searches" +
                "\nmrt      -   OPTIONAL    -> Search Type for Maturity Rating (i.e. equals, not equals) can range from 2 - 9 && 12 - 13" +
                "\"";
        }

        ViewObjectImpl stats1 = (ViewObjectImpl)applicationModule.findViewObject("UsageStatisticsVO1");

        String whereClause = "";

        if (request.getParameter("cname") != null) {
            if (whereClause.length() != 0) {
                whereClause += " AND ";
            }

            if (request.getParameter("cnamet") != null) {
                try {
                    int type = Integer.parseInt(request.getParameter("cnamet").toString());
                    if (type < 0 || type > 13) {
                        return "\"error\": {\"message\": \"Customer Name search type (cnamet) must be in range 0 to 13.\", \"status\": 400},";
                    }
                } catch (NumberFormatException e) {
                    return "\"error\": {\"message\": \"Customer Name search type (cnamet) is not a valid integer.\", \"status\": 400},";
                }
            }
            whereClause +=
                    "CustomerInfoEO.CUSTOMER_NAME" + makeWhere(request.getParameter("cnamet"), request.getParameter("cname"),
                                                               request.getParameter("cname2"));
        }

        if (request.getParameter("ind") != null) {
            if (whereClause.length() != 0) {
                whereClause += " AND ";
            }

            if (request.getParameter("indt") != null) {
                try {
                    int type = Integer.parseInt(request.getParameter("indt").toString());
                    if (type < 0 || type > 13) {
                        return "\"error\": {\"message\": \"Industry search type (indt) must be in range 0 to 13.\", \"status\": 400},";
                    }
                } catch (NumberFormatException e) {
                    return "\"error\": {\"message\": \"Industry search type (indt) is not a valid integer.\", \"status\": 400},";
                }
            }
            whereClause +=
                    "CustomerInfoEO.INDUSTRY" + makeWhere(request.getParameter("indt"), request.getParameter("ind"),
                                                          request.getParameter("ind2"));
        }

        if (request.getParameter("appe") != null) {
            if (whereClause.length() != 0) {
                whereClause += " AND ";
            }

            if (request.getParameter("appet") != null) {
                try {
                    int type = Integer.parseInt(request.getParameter("appet").toString());
                    if (type < 0 || type > 13) {
                        return "\"error\": {\"message\": \"Applicability Extent search type (appet) must be in range 0 to 13.\", \"status\": 400},";
                    }
                } catch (NumberFormatException e) {
                    return "\"error\": {\"message\": \"Applicability Extent search type (appet) is not a valid integer.\", \"status\": 400},";
                }
            }
            whereClause +=
                    "KadRegistrationEO.APPLICABILITY_EXTENT" + makeWhere(request.getParameter("appet"), request.getParameter("appe"),
                                                                         request.getParameter("appe2"));
        }

        if (request.getParameter("br") != null) {
            if (whereClause.length() != 0) {
                whereClause += " AND ";
            }

            if (request.getParameter("brt") != null) {
                try {
                    int type = Integer.parseInt(request.getParameter("brt").toString());
                    if (type < 0 || type > 13 || (type > 9 && type < 12)) {
                        return "\"error\": {\"message\": \"Benefit Rating search type (brt) must be in range 0 to 9 or 12 to 13.\", \"status\": 400},";
                    }
                } catch (NumberFormatException e) {
                    return "\"error\": {\"message\": \"Benefit Rating search type (brt) is not a valid integer.\", \"status\": 400},";
                }
            }
            whereClause +=
                    "KadRegistrationEO.BENEFIT_RATING" + makeWhere(request.getParameter("brt"), request.getParameter("br"),
                                                                   request.getParameter("br2"));
        }

        if (request.getParameter("c") != null) {
            if (whereClause.length() != 0) {
                whereClause += " AND ";
            }

            if (request.getParameter("ct") != null) {
                try {
                    int type = Integer.parseInt(request.getParameter("ct").toString());
                    if (type < 0 || type > 13) {
                        return "\"error\": {\"message\": \"Comments search type (ct) must be in range 0 to 13.\", \"status\": 400},";
                    }
                } catch (NumberFormatException e) {
                    return "\"error\": {\"message\": \"Comments search type (brt) is not a valid integer.\", \"status\": 400},";
                }
            }
            whereClause +=
                    "KadRegistrationEO.COMMENTS" + makeWhere(request.getParameter("ct"), request.getParameter("c"),
                                                             request.getParameter("c2"));
        }

        if (request.getParameter("ds") != null) {
            if (whereClause.length() != 0) {
                whereClause += " AND ";
            }

            if (request.getParameter("dst") != null) {
                try {
                    int type = Integer.parseInt(request.getParameter("dst").toString());
                    if (type < 0 || type > 13) {
                        return "\"error\": {\"message\": \"Deployment Status search type (dst) must be in range 0 to 9 or 12 to 13.\", \"status\": 400},";
                    }
                } catch (NumberFormatException e) {
                    return "\"error\": {\"message\": \"Deployment Status search type (dst) is not a valid integer.\", \"status\": 400},";
                }
            }
            whereClause +=
                    "KadRegistrationEO.DEPLOYMENT_STATUS" + makeWhere(request.getParameter("dst"), request.getParameter("ds"),
                                                                      request.getParameter("ds2"));
        }

        if (request.getParameter("k") != null) {
            if (whereClause.length() != 0) {
                whereClause += " AND ";
            }

            if (request.getParameter("kst") != null) {
                try {
                    int type = Integer.parseInt(request.getParameter("kst").toString());
                    if (type < 0 || type > 13 || (type > 9 && type < 12)) {
                        return "\"error\": {\"message\": \"Kad Id search type (kst) must be in range 0 to 9 or 12 to 13.\", \"status\": 400},";
                    }
                } catch (NumberFormatException e) {
                    return "\"error\": {\"message\": \"Kad Id search type (kst) is not a valid integer.\", \"status\": 400},";
                }
            }

            String resK = resolveKad(request.getParameter("k"), applicationModule);
            String resK2 = resolveKad(request.getParameter("k2"), applicationModule);

            if (resK == null)
                return "\"error\": {\"message\": \"Kad (k) cannot be resolved to an existing Kad.\", \"status\": 400},";

            if (resK2 == null &&
                (request.getParameter("kst").equals("8") || request.getParameter("kst").equals("9")))
                return "\"error\": {\"message\": \"Kad 2 (k2) cannot be resolved to an existing Kad.\", \"status\": 400},";

            whereClause += "KadRegistrationEO.KAD_ID" + makeWhere(request.getParameter("kst"), resK, resK2);
        }

        if (request.getParameter("mr") != null) {
            if (whereClause.length() != 0) {
                whereClause += " AND ";
            }

            if (request.getParameter("mrt") != null) {
                try {
                    int type = Integer.parseInt(request.getParameter("mrt").toString());
                    if (type < 0 || type > 13 || (type > 9 && type < 12)) {
                        return "\"error\": {\"message\": \"Maturity Rating search type (mrt) must be in range 0 to 9 or 12 to 13.\", \"status\": 400},";
                    }
                } catch (NumberFormatException e) {
                    return "\"error\": {\"message\": \"Maturity Rating search type (mrt) is not a valid integer.\", \"status\": 400},";
                }
            }
            whereClause +=
                    "KadRegistrationEO.MATURITY_RATING" + makeWhere(request.getParameter("mrt"), request.getParameter("mr"),
                                                                    request.getParameter("mr2"));
        }

        if (whereClause.length() != 0) {
            stats1.addWhereClause(whereClause);
        }
        stats1.executeQuery();
        RowSetIterator rs = stats1.createRowSetIterator(null);

        String data = "[";
        while (rs.hasNext()) {
            String entry = "{";
            ViewRowImpl res = (ViewRowImpl)rs.next();

            entry += "\"KadId\": \"" + res.getAttribute("KadId") + "\", ";
            entry += "\"Industry\": \"" + res.getAttribute("Industry") + "\", ";
            entry += "\"ApplicabilityExtent\": \"" + res.getAttribute("ApplicabilityExtent") + "\", ";
            entry += "\"BenefitRating\": \"" + res.getAttribute("BenefitRating") + "\", ";
            entry += "\"DeploymentStatus\": \"" + res.getAttribute("DeploymentStatus") + "\", ";
            entry += "\"MaturityRating\": \"" + res.getAttribute("MaturityRating") + "\", ";
            entry += "\"Comments\": \"" + res.getAttribute("Comments") + "\"";

            entry += "}";
            data += entry;
            if (rs.hasNext()) {
                data += ", ";
            }
        }
        data += "]";

        return "\"data\": " + data + ", \"status\": 200,";
    }

    /*

    q = 3 | Register KAD Usage
        k       -   Kad                 REQUIRED
        mr      -   MaturityRating      REQUIRED
        ds      -   DeploymentStatus    REQUIRED
        appe    -   ApplicabilityExtent REQUIRED
        br      -   BenefitRating       REQUIRED
        c       -   Comments            REQUIRED

     */

    private String registerKAD(ArchemySearchAM applicationModule, HttpServletRequest request, String user) {
        if (request.getParameter("h") != null) {
            return "\"data\": \"-Register Kad Usage- Query Type (q): 3. This API endpoint takes up to six (6) parameters, all of which are required. " +
                "\nThese parameters include:" + "\nPARAMETERS" + "\n - k   REQUIRED    -> Kad" +
                "\n - mr    REQUIRED    -> Maturity Rating (Integer)" + "\n - ds    REQUIRED    -> Deployment Status" +
                "\n - appe  REQUIRED    -> Applicability Extent" +
                "\n - br    REQUIRED    -> Benefit Rating (Integer)" + "\n - c     REQUIRED    -> Comments" + "\"";
        }
        ViewObjectImpl reg = (ViewObjectImpl)applicationModule.findViewObject("KadRegistrationVO1");
        reg.executeQuery();
        ViewRowImpl newRow = (ViewRowImpl)reg.createRow();

        newRow.setAttribute("UserId", user);

        if (request.getParameter("k") != null) {
            ArrayList<String> kads = getKads(applicationModule);
            boolean exists = false;
            String resolvedKad = resolveKad(request.getParameter("k"), applicationModule);
            if (resolvedKad == null)
                return "\"error\": {\"message\": \"Kad (k) cannot be resolved to exist.\", \"status\": 400},";
            for (String s : kads) {
                if (resolvedKad.equals(s.split(",")[0])) {
                    exists = true;
                }
            }
            if (exists) {
                newRow.setAttribute("KadId", resolvedKad);
            } else {
                return "\"error\": {\"message\": \"Kad (kid) specified does not exist in the DB.\", \"status\": 400},";
            }
        } else {
            return "\"error\": {\"message\": \"Kad (k) is not defined.\", \"status\": 400},";
        }

        if (request.getParameter("mr") != null) {
            newRow.setAttribute("MaturityRating", request.getParameter("mr"));
        } else {
            return "\"error\": {\"message\": \"Maturity Rating (mr) is not defined.\", \"status\": 400},";
        }

        if (request.getParameter("ds") != null) {
            newRow.setAttribute("DeploymentStatus", request.getParameter("ds"));
        } else {
            return "\"error\": {\"message\": \"Deployment Status (ds) is not defined.\", \"status\": 400},";
        }

        if (request.getParameter("appe") != null) {
            newRow.setAttribute("ApplicabilityExtent", request.getParameter("appe"));
        } else {
            return "\"error\": {\"message\": \"Applicability Extent (appe) is not defined.\", \"status\": 400},";
        }

        if (request.getParameter("br") != null) {
            newRow.setAttribute("BenefitRating", request.getParameter("br"));
        } else {
            return "\"error\": {\"message\": \"Benefit Rating (br) is not defined.\", \"status\": 400},";
        }

        if (request.getParameter("c") != null) {
            newRow.setAttribute("Comments", request.getParameter("c"));
        } else {
            return "\"error\": {\"message\": \"Comments (c) is not defined.\", \"status\": 400},";
        }

        reg.insertRow(newRow);

        try {
            newRow.getDBTransaction().commit();
            //            reg.getDBTransaction().commit();
        } catch (Exception e) {
            return "\"error\": {\"message\": \"" + e.getMessage() + "\", \"status\": 500},";
        }

        return "\"status\": 200,";
    }

    /*

     q = 4 | Customer Profile
        t       -   type (g for GET, s for SET) REQUIRED

        If SET is selected
        cname   -   Customer Name
        ind     -   Industry

    */

    private String custProfile(ArchemySearchAM applicationModule, HttpServletRequest request, String user) {
        if (request.getParameter("h") != null) {
            return "\"data\": \"-Customer Profile- Query Type (q): 4. This API endpoint takes up to three (3) parameters. Using this endpoint you can GET or SET your personal Customer Information such as Customer Name and Industry." +
                "\nThese parameters include:" + "\nPARAMETERS" +
                "\n - t     REQUIRED    -> Type of request (GET or SET)" + "\n - cname OPTIONAL    -> Customer Name" +
                "\n - ind   OPTIONAL    -> Industry" + "\"";
        }

        if (request.getParameter("t") == null) {
            return "\"error\": {\"message\": \"Type of operation is not defined for this query.\", \"status\": 400},";
        } else {
            if (request.getParameter("t").equals("g")) {
                CustomerInfoVOImpl customer = (CustomerInfoVOImpl)applicationModule.findViewObject("CustomerInfoVO1");
                customer.setbUserId(user);
                customer.executeQuery();
                RowSetIterator rs = customer.createRowSetIterator(null);
                while (rs.hasNext()) {
                    CustomerInfoVORowImpl res = (CustomerInfoVORowImpl)rs.next();
                    String result =
                        "{ \"name\": \"" + res.getCustomerName() + "\", \"industry\": \"" + res.getIndustry() + "\" }";
                    return "\"data\": " + result + ", \"status\": 200,";
                }
                return "\"status\": 200,";
            } else if (request.getParameter("t").equals("s")) {
                CustomerInfoVOImpl customer = (CustomerInfoVOImpl)applicationModule.findViewObject("CustomerInfoVO1");
                customer.setbUserId(user);
                customer.executeQuery();
                RowSetIterator rs = customer.createRowSetIterator(null);
                while (rs.hasNext()) {
                    CustomerInfoVORowImpl res = (CustomerInfoVORowImpl)rs.next();
                    if (request.getParameter("cname") != null) {
                        res.setCustomerName(request.getParameter("cname").toString());
                    }

                    if (request.getParameter("ind") != null) {
                        res.setIndustry(request.getParameter("ind").toString());
                    }

                    res.getDBTransaction().commit();
                }
                return "\"status\": 200,";
            } else {
                return "\"error\": {\"message\": \"Unknown operation type: " + request.getParameter("t") +
                    ". Expected (g) or (s).\", \"status\": 400},";
            }
        }
    }

    /*
     * Archemy Admin User Methods
     */

    /*

     q = 5 | Add KAD

        d       -   Domain              REQUIRED
        kn      -   Kad Name            REQUIRED
        kl      -   Kad Link            REQUIRED
        kpl     -   Kad Public Link     REQUIRED
        bp      -   Business Problem    REQUIRED

     */

    private String addKad(ArchemySearchAM applicationModule, HttpServletRequest request) {
        if (request.getParameter("h") != null) {
            return "\"data\": \"-Add Kad- Query Type (q): 5. This API endpoint takes five (5) parameters, all of which are required. Using this endpoint you can add a Kad to the knowledge repository." +
                "\nThese parameters include:" + "\nPARAMETERS" + "\n - d     REQUIRED    -> Domain" +
                "\n - kn    REQUIRED    -> Kad Name" + "\n - kl    REQUIRED    -> Kad Link" +
                "\n - kpl   REQUIRED    -> Kad Public Link" + "\n - bp    REQUIRED    -> Business Problem" + "\"";
        }
        KadTempVOImpl kadvo = (KadTempVOImpl)applicationModule.findViewObject("KadTempVO1");
        KadTempVORowImpl row = (KadTempVORowImpl)kadvo.createRow();

        if (request.getParameter("kn") == null) {
            return "\"error\": {\"message\": \"Kad Name (kn) is not specified.\", \"status\": 400},";
        } else {
            row.setKadName(request.getParameter("kn"));
        }

        if (request.getParameter("kl") == null) {
            return "\"error\": {\"message\": \"Kad Link (kl) is not specified.\", \"status\": 400},";
        } else {
            row.setKadLink(request.getParameter("kl"));
        }

        if (request.getParameter("kpl") == null) {
            return "\"error\": {\"message\": \"Kad Public Link (kpl) is not specified.\", \"status\": 400},";
        } else {
            row.setKadPublicLink(request.getParameter("kpl"));
        }

        if (request.getParameter("bp") == null) {
            return "\"error\": {\"message\": \"Business Problem (bp) is not specified.\", \"status\": 400},";
        } else {
            try {
                String resBP = resolveBP(request.getParameter("bp"), applicationModule);
                if (resBP == null)
                    return "\"error\": {\"message\": \"Business Problem (bp) cannot be resolved to exist.\", \"status\": 400},";
                row.setBusiness_problem(Integer.parseInt(resBP));
            } catch (NumberFormatException e) {
                return "\"error\": {\"message\": \"Business Problem is not a valid integer.\", \"status\": 400},";
            }
        }

        int domain = 0;
        if (request.getParameter("d") == null) {
            return "\"error\": {\"message\": \"Domain Id is not specified.\", \"status\": 400},";
        } else {
            try {
                String resD = resolveDomain(request.getParameter("d"), applicationModule);
                if (resD == null)
                    return "\"error\": {\"message\": \"Domain (d) cannot be resolved to exist.\", \"status\": 400},";
                domain = Integer.parseInt(resD);
            } catch (NumberFormatException e) {
                return "\"error\": {\"message\": \"Domain Id is not a valid integer.\", \"status\": 400},";
            }
        }

        boolean legal = false;

        ArrayList<String> domains = getDomains(applicationModule);
        for (String d : domains) {
            if ((domain + "").equals(d.split(",")[0])) {
                legal = true;
            }
        }

        if (legal) {
            kadvo.insertRow(row);
            applicationModule.addKAD(domain);
        } else {
            return "\"error\": {\"message\": \"Domain does not exist to add Kad into.\", \"status\": 400},";
        }

        return "\"status\": 200,";
    }

    /*

     q = 6 | Delete KAD

        k       -   Kad        REQUIRED

     */

    private String deleteKad(ArchemySearchAM applicationModule, HttpServletRequest request) {
        if (request.getParameter("h") != null) {
            return "\"data\": \"-Add Kad- Query Type (q): 6. This API endpoint takes one (1) parameter, which is required. Using this endpoint you can remove a Kad from the knowledge repository." +
                "\nThese parameters include:" + "\nPARAMETERS" + "\n - kid   REQUIRED    -> Kad Id" + "\"";
        }

        if (request.getParameter("k") == null) {
            return "\"error\": {\"message\": \"Kad Id is not specified.\", \"status\": 400},";
        } else {
            try {
                String resK = resolveKad(request.getParameter("k"), applicationModule);
                if (resK == null)
                    return "\"error\": {\"message\": \"Kad Id (kid) cannot be resolved to exist.\", \"status\": 400},";
                applicationModule.removeKAD(Integer.parseInt(resK));
            } catch (NumberFormatException e) {
                return "\"error\": {\"message\": \"Kad Id is not a valid integer.\", \"status\": 400},";
            }
        }

        return "\"status\": 200,";
    }

    /*

     q = 7 | Add Domain

        dn      -   Domain Name        REQUIRED
        dd      -   Domain Description

     */

    private String addDomain(ArchemySearchAM applicationModule, HttpServletRequest request) {
        if (request.getParameter("h") != null) {
            return "\"data\": \"-Add Domain- Query Type (q): 7. This API endpoint takes up to two (2) parameters. Using this endpoint you can add a Domain to the knowledge repository." +
                "\nThese parameters include:" + "\nPARAMETERS" + "\n - dn    REQUIRED    -> Domain Name" +
                "\n - dd    OPTIONAL    -> Domain Description" + "\"";
        }
        ViewObjectImpl domain = (ViewObjectImpl)applicationModule.findViewObject("DomainsVO1");
        ViewRowImpl newRow = (ViewRowImpl)domain.createRow();

        if (request.getParameter("dn") != null) {
            newRow.setAttribute("DomainName", request.getParameter("dn"));
        } else {
            return "\"error\": {\"message\": \"Domain Name is not defined.\", \"status\": 400},";
        }

        newRow.setAttribute("DomainDescription", request.getParameter("dd"));

        domain.insertRow(newRow);

        try {
            newRow.getDBTransaction().commit();
            //            reg.getDBTransaction().commit();
        } catch (Exception e) {
            return "\"error\": {\"message\": \"" + e.getMessage() + "\", \"status\": 500},";
        }

        return "\"status\": 200,";
    }

    /*

     q = 8 | Delete Domain

        d       -   Domain        REQUIRED

     */

    private String deleteDomain(ArchemySearchAM applicationModule, HttpServletRequest request) {
        if (request.getParameter("h") != null) {
            return "\"data\": \"-Delete Domain- Query Type (q): 8. This API endpoint takes one (1) parameter. Using this endpoint you can delete a Domain from the knowledge repository." +
                "\nThese parameters include:" + "\nPARAMETERS" + "\n - d    REQUIRED    -> Domain id" + "\"";
        }
        if (request.getParameter("d") == null) {
            return "\"error\": {\"message\": \"Domain (d) is not defined.\", \"status\": 400},";
        }

        ViewObjectImpl domain = (ViewObjectImpl)applicationModule.findViewObject("DomainsVO1");
        RowSetIterator rs = domain.createRowSetIterator(null);

        String resolvedDomain = resolveDomain(request.getParameter("d"), applicationModule);
        if (resolvedDomain == null)
            return "\"error\": {\"message\": \"Domain (d) cannot be resolved to exist.\", \"status\": 400},";

        while (rs.hasNext()) {
            Row row = rs.next();
            if (row.getAttribute("DomainId").toString().equals(resolvedDomain)) {
                rs.removeCurrentRow();
            }
        }

        try {
            domain.getDBTransaction().commit();
            //            reg.getDBTransaction().commit();
        } catch (Exception e) {
            return "\"error\": {\"message\": \"" + e.getMessage() + "\", \"status\": 500},";
        }

        return "\"status\": 200,";
    }

    /*

     q = 9 | Add Dimension

        dn      -   Dimension Name      REQUIRED
        d       -   Domain              REQUIRED

     */

    private String addDimension(ArchemySearchAM applicationModule, HttpServletRequest request) {
        if (request.getParameter("h") != null) {
            return "\"data\": \"-Add Dimension- Query Type (q): 9. This API endpoint takes two (2) parameters, both of which are required. Using this endpoint you can add a Dimension to the knowledge repository." +
                "\nThese parameters include:" + "\nPARAMETERS" + "\n - dn    REQUIRED    -> Dimension Name" +
                "\n - d    REQUIRED    -> Domain" + "\"";
        }

        ViewObjectImpl dim = (ViewObjectImpl)applicationModule.findViewObject("DimensionsVO1");
        ViewRowImpl newRow = (ViewRowImpl)dim.createRow();

        if (request.getParameter("dn") != null) {
            newRow.setAttribute("DimensionName", request.getParameter("dn"));
        } else {
            return "\"error\": {\"message\": \"Dimension Name is not defined.\", \"status\": 400},";
        }

        String resolvedDomain = resolveDomain(request.getParameter("d"), applicationModule);
        if (resolvedDomain == null)
            return "\"error\": {\"message\": \"Domain (d) cannot be resolved to exist. " + request.getParameter("d") + " \", \"status\": 400},";

        if (request.getParameter("d") != null) {
            try {
                newRow.setAttribute("DomainId", Integer.parseInt(resolvedDomain));
            } catch (NumberFormatException e) {
                return "\"error\": {\"message\": \"Domain (d) does not correspond to a valid Domain.\", \"status\": 400},";
            }
        } else {
            return "\"error\": {\"message\": \"Domain (d) is not defined.\", \"status\": 400},";
        }

        ArrayList<String> domains = getDomains(applicationModule);
        boolean legal = false;
        for (String d : domains) {
            if (resolvedDomain.equals(d.split(",")[0])) {
                legal = true;
            }
        }

        if (legal) {
            dim.insertRow(newRow);
        } else {
            return "\"error\": {\"message\": \"Domain does not exist to insert Dimension into.\", \"status\": 400},";
        }

        try {
            newRow.getDBTransaction().commit();
            //            reg.getDBTransaction().commit();
        } catch (Exception e) {
            return "\"error\": {\"message\": \"" + e.getMessage() + " " + resolvedDomain + " " +
                request.getParameter("d") + "\", \"status\": 500},";
        }

        return "\"status\": 200,";
    }

    /*

     q = 10 | Delete Dimension

        d       -   Dimension        REQUIRED

     */

    private String deleteDimension(ArchemySearchAM applicationModule, HttpServletRequest request) {
        if (request.getParameter("h") != null) {
            return "\"data\": \"-Delete Dimension- Query Type (q): 10. This API endpoint takes one (1) parameters, which is required. Using this endpoint you can delete a Dimension from the knowledge repository." +
                "\nThese parameters include:" + "\nPARAMETERS" + "\n - id    REQUIRED    -> Dimension Id" + "\"";
        }

        if (request.getParameter("d") == null) {
            return "\"error\": {\"message\": \"Dimension (d) is not defined.\", \"status\": 400},";
        }

        ViewObjectImpl dim = (ViewObjectImpl)applicationModule.findViewObject("DimensionsVO1");
        RowSetIterator rs = dim.createRowSetIterator(null);

        String resolvedDimension = resolveDimension(request.getParameter("d"), applicationModule);
        if (resolvedDimension == null)
            return "\"error\": {\"message\": \"Dimension (d) cannot be resolved to exist.\", \"status\": 400},";

        while (rs.hasNext()) {
            Row row = rs.next();
            if (row.getAttribute("DimensionId").toString().equals(resolvedDimension)) {
                rs.removeCurrentRow();
            }
        }

        try {
            dim.getDBTransaction().commit();
            //            reg.getDBTransaction().commit();
        } catch (Exception e) {
            return "\"error\": {\"message\": \"" + e.getMessage() + "\", \"status\": 500},";
        }

        return "\"status\": 200,";
    }

    /*

     q = 11 | Add Area

        ap          -   Area Parent             
        aon         -   Area Order Number       REQUIRED
        adl         -   Area Depth Level        REQUIRED
        d           -   Dimension               REQUIRED
        an          -   Area Name               REQUIRED

     */

    private String addArea(ArchemySearchAM applicationModule, HttpServletRequest request) {
        if (request.getParameter("h") != null) {
            return "\"data\": \"-Delete Dimension- Query Type (q): 11. This API endpoint takes up to five (5) parameters. Using this endpoint you can add an Area to the knowledge repository." +
                "\nThese parameters include:" + "\nPARAMETERS" + "\n - ap  OPTIONAL    -> Area Parent" +
                "\n - aon   REQUIRED    -> Area Order Number" + "\n - adl   REQUIRED    -> Area Depth Level" +
                "\n - d    REQUIRED    -> Dimension Id" + "\n - an    REQUIRED    -> Area Name" + "\"";
        }

        String resDim = resolveDimension(request.getParameter("d"), applicationModule);
        String resAPID = resolveArea(request.getParameter("ap"), applicationModule);

        if (resDim == null)
            return "\"error\": {\"message\": \"Dimension (d) cannot be resolved to exist.\", \"status\": 400},";
        if (request.getParameter("ap") != null && resAPID == null)
            return "\"error\": {\"message\": \"Area Parent (ap) cannot be resolved to exist.\", \"status\": 400},";

        ViewObjectImpl area = (ViewObjectImpl)applicationModule.findViewObject("AreasVO2");
        ViewRowImpl newRow = (ViewRowImpl)area.createRow();

        if (request.getParameter("ap") != null) {
            try {
                newRow.setAttribute("AreaParentId", Integer.parseInt(resAPID));
            } catch (NumberFormatException e) {
                return "\"error\": {\"message\": \"Area Parent (ap) is not a valid Integer.\", \"status\": 400},";
            }
        } else {
            newRow.setAttribute("AreaParentId", null);
        }

        if (request.getParameter("aon") != null) {
            try {
                newRow.setAttribute("AreaOrderNo", Integer.parseInt(request.getParameter("aon")));
            } catch (NumberFormatException e) {
                return "\"error\": {\"message\": \"Area Order Number (aon) is not a valid Integer.\", \"status\": 400},";
            }
        } else {
            return "\"error\": {\"message\": \"Area Order Number (aon) is not defined.\", \"status\": 400},";
        }

        if (request.getParameter("adl") != null) {
            try {
                newRow.setAttribute("AreaDepthLevel", Integer.parseInt(request.getParameter("adl")));
            } catch (NumberFormatException e) {
                return "\"error\": {\"message\": \"Area Depth Level(adl) is not a valid Integer.\", \"status\": 400},";
            }
        } else {
            return "\"error\": {\"message\": \"Area Depth Level(adl) is not defined.\", \"status\": 400},";
        }

        if (request.getParameter("d") != null) {
            try {
                newRow.setAttribute("DimensionId", Integer.parseInt(resDim));
            } catch (NumberFormatException e) {
                return "\"error\": {\"message\": \"Dimension (d) is not a valid Integer.\", \"status\": 400},";
            }
        } else {
            return "\"error\": {\"message\": \"Dimension (d) is not defined.\", \"status\": 400},";
        }

        if (request.getParameter("an") != null) {
            newRow.setAttribute("AreaName", request.getParameter("an"));
        } else {
            return "\"error\": {\"message\": \"Area Name (an) is not defined.\", \"status\": 400},";
        }

        ArrayList<String> dimensions = getDimensions(applicationModule);
        boolean legalDim = false;
        for (String d : dimensions) {
            if (resDim.equals(d.split(",")[0])) {
                legalDim = true;
            }
        }

        boolean legalParent = false;
        if (request.getParameter("ap") == null)
            legalParent = true;
        else {
            ArrayList<String> areas = getAreas(applicationModule);
            for (String a : areas) {
                if (resAPID.equals(a.split(",")[0])) {
                    legalParent = true;
                }
            }
        }

        if (legalDim && legalParent) {
            area.insertRow(newRow);
        } else {
            return "\"error\": {\"message\": \"Dimension or Parent Area is not legally defined.\", \"status\": 400},";
        }

        try {
            newRow.getDBTransaction().commit();
            //            reg.getDBTransaction().commit();
        } catch (Exception e) {
            return "\"error\": {\"message\": \"" + e.getMessage() + "\", \"status\": 500},";
        }

        return "\"status\": 200,";
    }

    /*

     q = 12 | Delete Area

        a       -   Area        REQUIRED

     */

    private String deleteArea(ArchemySearchAM applicationModule, HttpServletRequest request) {
        if (request.getParameter("h") != null) {
            return "\"data\": \"-Delete Dimension- Query Type (q): 12. This API endpoint takes one (1) parameter. Using this endpoint you can delete an Area from the knowledge repository." +
                "\nThese parameters include:" + "\nPARAMETERS" + "\n - id    REQUIRED    -> Area Id" + "\"";
        }
        if (request.getParameter("a") == null) {
            return "\"error\": {\"message\": \"Area (a) is not defined.\", \"status\": 400},";
        }

        ViewObjectImpl dim = (ViewObjectImpl)applicationModule.findViewObject("AreasVO2");
        RowSetIterator rs = dim.createRowSetIterator(null);

        String resolvedArea = resolveArea(request.getParameter("a"), applicationModule);
        if (resolvedArea == null)
            return "\"error\": {\"message\": \"Area (id) cannot be resolved to exist.\", \"status\": 400},";

        while (rs.hasNext()) {
            Row row = rs.next();
            if (row.getAttribute("AreaId").toString().equals(resolvedArea)) {
                rs.removeCurrentRow();
            }
        }

        try {
            dim.getDBTransaction().commit();
            //            reg.getDBTransaction().commit();
        } catch (Exception e) {
            return "\"error\": {\"message\": \"" + e.getMessage() + "\", \"status\": 500},";
        }

        return "\"status\": 200,";
    }

    /*

     q = 13 | Add Business Problem

        bp      -   Business Problem    REQUIRED
        c       -   Context             REQUIRED
        d       -   Description         REQUIRED
        t       -   Type                REQUIRED

     */

    private String addBP(ArchemySearchAM applicationModule, HttpServletRequest request) {
        if (request.getParameter("h") != null) {
            return "\"data\": \"-Add Business Problem- Query Type (q): 13. This API endpoint takes four (4) parameters, all of which are required. Using this endpoint you can add a Business Problem to the knowledge repository." +
                "\nThese parameters include:" + "\nPARAMETERS" + "\n - bp    REQUIRED    -> Business Problem" +
                "\n - c     REQUIRED    -> Context" + "\n - d     REQUIRED    -> Description" +
                "\n - t     REQUIRED    -> Type" + "\"";
        }
        ViewObjectImpl bp = (ViewObjectImpl)applicationModule.findViewObject("RecurringBusProblemVO2");
        ViewRowImpl newRow = (ViewRowImpl)bp.createRow();

        if (request.getParameter("bp") != null) {
            newRow.setAttribute("BusinessProblem", request.getParameter("bp"));
        } else {
            return "\"error\": {\"message\": \"Business Problem (bp) is not defined.\", \"status\": 400},";
        }

        if (request.getParameter("c") != null) {
            newRow.setAttribute("Context", request.getParameter("c"));
        } else {
            return "\"error\": {\"message\": \"Context (c) is not defined.\", \"status\": 400},";
        }

        if (request.getParameter("d") != null) {
            newRow.setAttribute("Description", request.getParameter("d"));
        } else {
            return "\"error\": {\"message\": \"Description (d) is not defined.\", \"status\": 400},";
        }

        if (request.getParameter("t") != null) {
            newRow.setAttribute("Type", request.getParameter("t"));
        } else {
            return "\"error\": {\"message\": \"Type (t) is not defined.\", \"status\": 400},";
        }

        bp.insertRow(newRow);

        try {
            newRow.getDBTransaction().commit();
            //            reg.getDBTransaction().commit();
        } catch (Exception e) {
            return "\"error\": {\"message\": \"" + e.getMessage() + "\", \"status\": 500},";
        }

        return "\"status\": 200,";
    }

    /*

     q = 14 | Delete Business Problem

        bp       -   Business Problem        REQUIRED

     */

    private String deleteBP(ArchemySearchAM applicationModule, HttpServletRequest request) {
        if (request.getParameter("h") != null) {
            return "\"data\": \"-Add Business Problem- Query Type (q): 14. This API endpoint takes one (1) parameter, which is required. Using this endpoint you can delete a Business Problem from the knowledge repository." +
                "\nThese parameters include:" + "\nPARAMETERS" + "\n - id    REQUIRED    -> Business Problem Id" +
                "\"";
        }
        if (request.getParameter("bp") == null) {
            return "\"error\": {\"message\": \"Business Problem (bp) is not defined.\", \"status\": 400},";
        }

        ViewObjectImpl bp = (ViewObjectImpl)applicationModule.findViewObject("RecurringBusProblemVO2");
        RowSetIterator rs = bp.createRowSetIterator(null);

        String resolvedBP = resolveBP(request.getParameter("bp"), applicationModule);
        if (resolvedBP == null)
            return "\"error\": {\"message\": \"Business Problem (bp) cannot be resolved to exist.\", \"status\": 400},";

        while (rs.hasNext()) {
            Row row = rs.next();
            if (row.getAttribute("Id").toString().equals(resolvedBP)) {
                rs.removeCurrentRow();
            }
        }

        try {
            bp.getDBTransaction().commit();
            //            reg.getDBTransaction().commit();
        } catch (Exception e) {
            return "\"error\": {\"message\": \"" + e.getMessage() + "\", \"status\": 500},";
        }

        return "\"status\": 200,";
    }

    /*

     q = 15 | Get Customer Info

        uid       -   User Id        REQUIRED

     */

    private String getCustInfo(ArchemySearchAM applicationModule, HttpServletRequest request) {
        if (request.getParameter("h") != null) {
            return "\"data\": \"-Add Business Problem- Query Type (q): 15. This API endpoint takes one (1) parameter, which is required. Using this endpoint you can view a user's profile." +
                "\nThese parameters include:" + "\nPARAMETERS" +
                "\n - uid   REQUIRED    -> User Id to find within the database" + "\"";
        }
        if (request.getParameter("uid") == null) {
            return "\"error\": {\"message\": \"User Id is not defined.\", \"status\": 400},";
        }

        ViewObjectImpl users = (ViewObjectImpl)applicationModule.findViewObject("CustomerInfoAllVO1");

        RowSetIterator rs = users.createRowSetIterator(null);

        while (rs.hasNext()) {
            Row row = rs.next();
            if (row.getAttribute("UserId").toString().equals(request.getParameter("uid"))) {
                return "\"data\": {\"Name\": \"" + row.getAttribute("CustomerName") + "\", \"Industry\": \"" +
                    row.getAttribute("Industry") + "\" }, \"status\": 200,";
            }
        }
        return "\"status\": 200, \"message\": \"User not found.\",";
    }

    private int APILogin(ArchemySearchAM applicationModule, String username, char[] pass, String[] messages) {
        FortressSecurityUtil ops = new FortressSecurityUtil();
        AccessMgr acMgr = ops.createAndGetAccessMgr();
        User user = new User();
        user.setUserId(username);
        user.setPassword(pass);
        int perm = -1;

        try {
            Session rbacSession = acMgr.createSession(user, false);
            if (rbacSession.getRoles().iterator().hasNext()) {
                perm = 0;
            }
            if (rbacSession.getAdminRoles().iterator().hasNext()) {
                perm = 1;
            }
            if (rbacSession != null) {
                //only for normal users should customer information form be there
                if (rbacSession.getAdminRoles().isEmpty()) {
                    applicationModule.addCustomerRowIfNotExists(username);
                }
            }
            //check for warnings if they contain password expiration warning warn him of the same
            //and provide option to change the password
            List<Warning> warnings = rbacSession.getWarnings();
            String ws = "\"warnings\": [";
            if (warnings != null) {
                for (Warning w : warnings) {
                    if (w != null) {
                        ws += w.getMsg() + ",";
                    }
                }
                if (ws.length() > 1) {
                    ws = ws.substring(0, ws.length() - 1);
                }
            }
            ws += "],";
            messages[0] = ws;

        } catch (PasswordException e) {
            //this means the user's password was reset by admin and hence redirect user to change his password afterward you can log him out
            if (e.getErrorId() != GlobalErrIds.USER_PW_RESET) {
                messages[1] =
                        "\"error\": {\"message\": \"Login Failed due to authentication failure " + e.getMessage() +
                        "\", \"status\": 400},";
                return -1;
            }

        } catch (ValidationException e) {
            messages[1] =
                    "\"error\": {\"message\": \"User is not allowed access " + e.getMessage() + "\", \"status\": 400},";
            return -1;
        } catch (FinderException e) {
            messages[1] = "\"error\": {\"message\": \"User does not exist " + e.getMessage() + "\", \"status\": 400},";
            return -1;
        } catch (SecurityException e) {
            messages[1] =
                    "\"error\": {\"message\": \"Authentication failed due to " + e.getMessage() + "\", \"status\": 400},";
            return -1;
        }
        return perm;
    }

    public static Key loadPrivateKey(String key64) throws GeneralSecurityException, IOException {
        byte[] clear = DatatypeConverter.parseBase64Binary(key64);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
        KeyFactory fact = KeyFactory.getInstance("RSA");
        PrivateKey priv = fact.generatePrivate(keySpec);
        Arrays.fill(clear, (byte)0);
        return priv;
    }

    public static String decrypt(String data, Key privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException,
                                                                     InvalidKeyException, BadPaddingException,
                                                                     IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(cipher.doFinal(DatatypeConverter.parseBase64Binary(data)));
    }

    /*
        Search Type Chart (sRange):
        ---------------------------
        0 - Starts with
        1 - Ends with
        2 - Equals
        3 - Does not equal
        4 - Less than
        5 - Less than or equal to
        6 - Greater than
        7 - Greater than or equal to
        8 - Between
        9 - Not between
        10 - Contains
        11 - Does not contain
        12 - Is blank
        13 - Is not blank
     */

    private String makeWhere(String num, String s1, String s2) {

        //s1 will always be non null because we check s1 before the call is made

        if (s2 == null) {
            s2 = "";
        }

        if (num == null) {
            return " = '" + s1 + "'";
        }

        switch (num) {
        case "0":
            return " LIKE '" + s1 + "%'";
        case "1":
            return " LIKE '%" + s1 + "'";
        case "2":
            return " = '" + s1 + "'";
        case "3":
            return " != '" + s1 + "'";
        case "4":
            return " < '" + s1 + "'";
        case "5":
            return " <= '" + s1 + "'";
        case "6":
            return " > '" + s1 + "'";
        case "7":
            return " >= '" + s1 + "'";
        case "8":
            return " BETWEEN '" + s1 + "' AND '" + s2 + "'";
        case "9":
            return " NOT BETWEEN '" + s1 + "' AND '" + s2 + "'";
        case "10":
            return " LIKE '%" + s1 + "%'";
        case "11":
            return " NOT LIKE '%" + s1 + "%'";
        case "12":
            return " = ''";
        case "13":
            return " LIKE '_%'";
        default:
            return "Error";
        }
    }

    private String commandLine(ArchemySearchAM applicationModule, HttpServletRequest request) {
        try {
            String privKey =
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCPmv0zyS5JQr6INSYBdg+/qMm6mxE3AwtltwIGn3zt0y6r2e0RUH8+jKlli1yNLcT/epcxGxPigztCM8wSKWuzMbdEz1ED0pxaz/ba4hlmLlHiRAUG9nqb7mKVz1qlZZY11Rjkcsg5eA5gAdYEclMIr7eYenrQSEbaK4wSuyOLD1XH0f5a+UvudnuteksA8j5fkwsr7n2IvWIxaOzDlYAj5mQPALQYbQNFUllIUTJJkkQC/PHrnUOrTxdnFzgs+oQWtkEbENnE6ZKs08dk8jM4du/hmKL0sCI7DqAIwBT7UMCF8fT1gOsEC+hVbFTQbnTiRi2X1rhZQZqUbKn/hEbRAgMBAAECggEADgshIdRVw3JUgat46QGrrpmKCMarW07f6XWJLC6in/tcABBSv7O4jdxhoH2Ncnz8W+OYL4QvYKJmxCWemlQUpTSCcKc5i/8nrTXTNTqRM03qUg9G0pR+Dwuz9mSNv8j8dI0/Xu/epsgX18m2LT8k4Z+Ve8LWidHXo/RIQXitlCapBuDDoceD6WhI+iVoRGhLaSrzR+f04Sn58l735h1OuR6QPxIY9D1t+ox0adko/6m4NEsg7MkGDOXKwIaPRlEPojB115OnfCmZQIyoz0JcSUB5MdVIR33YTLiFryw01vQFUC7lBEqUV1cS7sYj35yUV2rylNkEkUk6B6fC+EXchQKBgQDWid5E3f70vEGBkMRzrQO9CFiocmkl8mDnS8heMsrSP6wfEjsm/Q4f70KBVMnzFzESfshNtvuiwYUdOi8+V9xFS9qJkO4hoHVCcbHqP6GEmrRJUEuP29zu9urwtINGhCoEcMm2DLOEM8wuQX5hDHe8wULADzmNoG8vJOM93uImFwKBgQCrW8IsgTTANidNBLBP9zMYluyOoPIoXnIV00Gju4X7fD4MrmNeR35GcAxKQiegoHniTqroCJ2FWuqLo136W1DlY1ws0C1hwE/T//BmKAZI70uZp0qDggg2zG9SgwQgLyNeZ/4OnCBW9X/8T5AO7+x+2YCcoraYMiO7kVZt1/tzVwKBgEmGHkKDwiili92Xe3wZQzq5bYjtDNQQaN1bv2NpDNFZOOe9G8CU4Q5YtPYV1NAWlp68DHF10G9K2w/VLPO0sKye/lo+7R1hHE6VIGAjRntneXnWps66jtDmlkW/122HRc8XyEk3uR4JkmQX1fP0jeSGZxXjIdpDrVb+0VIW3HIpAoGAECag4ZL4BtnT0HWNrKvO/BVVjIfs6xMjy5zSxfzpvu9R5d4V7Y/tffQXpHQhygj2E/d4MlCFkEkmbCzksbEjqcs4p9yjOmBm5cNsxCQnm346cOwMoOKDpa6VG4DPxbzLp51Dm9rpTWjsPDq/iDji4H3dmmXXsfaf2ZD0RXwi7hcCgYEAghxb2DbIGxXkgiB5rNgu041yN2ABzcORQDEqUXnmK8+1hPtPi6rvoGKeGwAEP6HLNRrJG9n9XnxRWV6MqbyJjKF5YSJVddrj4CXDYBAVvaRRHePAtA7cLrysBOGj0krxyDzZY6i64mB/jb5FxaVkABCKqhz9wR2hRLc3liH23gk=";
            String decoded = decrypt(request.getParameter("key"), loadPrivateKey(privKey));
            privKey = null;
            String user = decoded.split("[|]")[0];
            String[] add = new String[] { "", "" };

            String JSON = "{";

            int perms = APILogin(applicationModule, user, decoded.split("[|]")[1].toCharArray(), add);

            JSON += add[0];
            if (!add[1].equals("")) {
                JSON += add[1];
            }

            if (perms == -1)
                return (JSON.endsWith(",") ? JSON.substring(0, JSON.length() - 1) : JSON) + "}";

            int[] listPerms = callPerms(perms);
            JSON += "\"Perms\": [";
            for (int i = 0; i < listPerms.length; i++) {
                JSON += "\"" + listPerms[i] + "," + mapPermToMethod(listPerms[i]) + "\"";
                if (i < listPerms.length - 1) {
                    JSON += ",";
                }
            }
            JSON += "]";
            /*
             * Each Domain has a set of Dimensions
             * Each Dimension has a set of Areas
             * Each Area has a set of Child Areas
             * Need Business Problems too
             */

            JSON += ", \"BPs\": [";
            ArrayList<String> arr = getBPs(applicationModule);
            for (int i = 0; i < arr.size(); i++) {
                JSON += "\"" + arr.get(i) + "\"";
                if (i < arr.size() - 1) {
                    JSON += ",";
                }
            }
            JSON += "]";

            JSON += ", \"Domains\": [";
            arr = getDomains(applicationModule);
            for (int i = 0; i < arr.size(); i++) {
                JSON += "\"" + arr.get(i) + "\"";
                if (i < arr.size() - 1) {
                    JSON += ",";
                }
            }
            JSON += "]";

            JSON += ", \"Dimensions\": [";
            arr = getDimensions(applicationModule);
            for (int i = 0; i < arr.size(); i++) {
                JSON += "\"" + arr.get(i) + "\"";
                if (i < arr.size() - 1) {
                    JSON += ",";
                }
            }
            JSON += "]";

            JSON += ", \"Areas\": [";
            arr = getAreas(applicationModule);
            for (int i = 0; i < arr.size(); i++) {
                JSON += "\"" + arr.get(i) + "\"";
                if (i < arr.size() - 1) {
                    JSON += ",";
                }
            }
            JSON += "]";

            JSON += ", \"Kads\": [";
            arr = getKads(applicationModule);
            for (int i = 0; i < arr.size(); i++) {
                JSON += "\"" + arr.get(i) + "\"";
                if (i < arr.size() - 1) {
                    JSON += ",";
                }
            }
            JSON += "]";

            arr = null;

            String[] sChart = searchChart();

            JSON += ", \"Search Types\": [";
            for (int i = 0; i < sChart.length; i++) {
                JSON += "\"" + sChart[i] + "\"";
                if (i < sChart.length - 1) {
                    JSON += ",";
                }
            }
            JSON += "]";

            JSON += "}";
            return JSON;
        } catch (Exception e) {
            return "\"error\": {\"message\": \"" + e.getMessage() + "\", \"status\": 500},";
        }
    }

    private int[] callPerms(int authLevel) {
        switch (authLevel) {
        case -1:
            return new int[] { };
        case 0:
            return new int[] { 0, 1, 2, 3, 4 };
        case 1:
            return new int[] { 0, 1, 2, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
        default:
            return new int[] { };
        }
    }

    private String mapPermToMethod(int perm) {
        switch (perm) {
        case 0:
            return "Search KAD";
        case 1:
            return "Search Catalog";
        case 2:
            return "View Usage Statistics";
        case 3:
            return "Register KAD Usage";
        case 4:
            return "View Your Profile";
        case 5:
            return "Add KAD";
        case 6:
            return "Delete KAD";
        case 7:
            return "Add Domain";
        case 8:
            return "Delete Domain";
        case 9:
            return "Add Dimension";
        case 10:
            return "Delete Dimension";
        case 11:
            return "Add Area";
        case 12:
            return "Delete Area";
        case 13:
            return "Add Business Problem";
        case 14:
            return "Delete Business Problem";
        case 15:
            return "Get Customer Info";
        default:
            return "";
        }
    }

    private ArrayList<String> getDomains(ArchemySearchAM applicationModule) {
        ArrayList<String> arr = new ArrayList<String>();
        ViewObjectImpl domains = (ViewObjectImpl)applicationModule.findViewObject("DomainsVO2");

        RowSetIterator rs = domains.createRowSetIterator(null);
        while (rs.hasNext()) {
            Row row = rs.next();
            arr.add(row.getAttribute("DomainId") + "," + row.getAttribute("DomainName") + "," +
                    row.getAttribute("DomainDescription"));
        }
        return arr;
    }

    private ArrayList<String> getDimensions(ArchemySearchAM applicationModule) {
        ArrayList<String> arr = new ArrayList<String>();
        ViewObjectImpl dims = (ViewObjectImpl)applicationModule.findViewObject("DimensionsVO2");

        RowSetIterator rs = dims.createRowSetIterator(null);
        while (rs.hasNext()) {
            Row row = rs.next();
            arr.add(row.getAttribute("DimensionId") + "," + row.getAttribute("DimensionName") + "," +
                    row.getAttribute("DomainId"));
        }
        return arr;
    }

    private ArrayList<String> getAreas(ArchemySearchAM applicationModule) {
        ArrayList<String> arr = new ArrayList<String>();
        ViewObjectImpl areas = (ViewObjectImpl)applicationModule.findViewObject("AreasVO2");

        RowSetIterator rs = areas.createRowSetIterator(null);
        while (rs.hasNext()) {
            Row row = rs.next();
            arr.add(row.getAttribute("AreaId") + "," + row.getAttribute("AreaName") + "," +
                    row.getAttribute("DimensionId") + "," + row.getAttribute("AreaParentId"));
        }
        return arr;
    }

    private ArrayList<String> getBPs(ArchemySearchAM applicationModule) {
        ArrayList<String> arr = new ArrayList<String>();
        ViewObjectImpl bps = (ViewObjectImpl)applicationModule.findViewObject("RecurringBusProblemVO2");
        RowSetIterator rs = bps.createRowSetIterator(null);
        while (rs.hasNext()) {
            Row row = rs.next();
            arr.add(row.getAttribute("Id") + "," + row.getAttribute("BusinessProblem") + "," +
                    row.getAttribute("Description"));
        }
        return arr;
    }

    private ArrayList<String> getKads(ArchemySearchAM applicationModule) {
        ArrayList<String> arr = new ArrayList<String>();
        ViewObjectImpl bps = (ViewObjectImpl)applicationModule.findViewObject("KadsVO3");
        RowSetIterator rs = bps.createRowSetIterator(null);
        while (rs.hasNext()) {
            Row row = rs.next();
            arr.add(row.getAttribute("KadId") + "," + row.getAttribute("KadName"));
        }
        return arr;
    }

    private String[] searchChart() {
        return new String[] { "0,Starts with", "1,Ends with", "2,Equals", "3,Does not equal", "4,Less than",
                              "5,Less than or equal to", "6,Greater than", "7,Greater than or equal to", "8,Between",
                              "9,Not between", "10,Contains", "11,Does not contain", "12,Is blank",
                              "13,Is not blank" };
    }

    private String resolveKad(String s1, ArchemySearchAM applicationModule) {
        if (s1 == null)
            return null;

        Map<String, Object> names = new HashMap<String, Object>();
        ArrayList<String> kads = getKads(applicationModule);
        for (String entry : kads) {
            names.put(entry.split(",")[1], entry.split(",")[0]);
        }
        for (String entry : kads) {
            names.put(entry.split(",")[0], entry.split(",")[0]);
        }
        return (names.get(s1) == null ? s1 : names.get(s1).toString());
    }

    private String resolveDomain(String s1, ArchemySearchAM applicationModule) {
        if (s1 == null)
            return null;

        Map<String, Object> names = new HashMap<String, Object>();
        ArrayList<String> kads = getDomains(applicationModule);
        for (String entry : kads) {
            names.put(entry.split(",")[1], entry.split(",")[0]);
        }
        for (String entry : kads) {
            names.put(entry.split(",")[0], entry.split(",")[0]);
        }
        return (names.get(s1) == null ? null : names.get(s1).toString());
    }

    private String resolveDimension(String s1, ArchemySearchAM applicationModule) {
        if (s1 == null)
            return null;

        Map<String, Object> names = new HashMap<String, Object>();
        ArrayList<String> kads = getDimensions(applicationModule);
        for (String entry : kads) {
            names.put(entry.split(",")[1], entry.split(",")[0]);
        }
        for (String entry : kads) {
            names.put(entry.split(",")[0], entry.split(",")[0]);
        }
        return (names.get(s1) == null ? s1 : names.get(s1).toString());
    }

    private String resolveArea(String s1, ArchemySearchAM applicationModule) {
        if (s1 == null)
            return null;

        Map<String, Object> names = new HashMap<String, Object>();
        ArrayList<String> kads = getAreas(applicationModule);
        for (String entry : kads) {
            names.put(entry.split(",")[1], entry.split(",")[0]);
        }
        for (String entry : kads) {
            names.put(entry.split(",")[0], entry.split(",")[0]);
        }
        return (names.get(s1) == null ? s1 : names.get(s1).toString());
    }

    private String resolveBP(String s1, ArchemySearchAM applicationModule) {
        if (s1 == null)
            return null;

        Map<String, Object> names = new HashMap<String, Object>();
        ArrayList<String> kads = getBPs(applicationModule);
        for (String entry : kads) {
            names.put(entry.split(",")[1], entry.split(",")[0]);
        }
        for (String entry : kads) {
            names.put(entry.split(",")[0], entry.split(",")[0]);
        }
        return (names.get(s1) == null ? s1 : names.get(s1).toString());
    }
}