package com.archemy.searchapp.view.beans;


import com.archemy.searchapp.model.lookups.DimensionsFilteredByDomainVOImpl;
import com.archemy.searchapp.model.lookups.DomainLookupVORowImpl;
import com.archemy.searchapp.model.queries.KADDimensionsAreaTempVORowImpl;
import com.archemy.searchapp.model.queries.KadSearchTransVORowImpl;
import com.archemy.searchapp.model.queries.SummaryUsageStatisticsVOImpl;

import com.blogspot.ramannanda.adf.utils.adffacesutils.ADFUtils;
import com.blogspot.ramannanda.adf.utils.adffacesutils.JSFUtils;

import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.share.logging.ADFLogger;
import oracle.adf.view.rich.component.rich.RichPopup;
import oracle.adf.view.rich.component.rich.data.RichTable;
import oracle.adf.view.rich.context.AdfFacesContext;

import oracle.binding.OperationBinding;

import oracle.jbo.RowSetIterator;

import org.apache.myfaces.trinidad.render.ExtendedRenderKitService;
import org.apache.myfaces.trinidad.util.Service;


/**
 * This class is a managed bean for searching and adding KAD
 */
public class SearchAndAddKADBacking {
  private static final ADFLogger logger = ADFLogger.createADFLogger(SearchAndAddKADBacking.class);
  private Integer domainID;
  private Integer businessProblem;
  private RichPopup addPopup;
  private RichPopup summaryPopup;
  private Integer remainingWeight=100;
  private RichTable table1;

  public SearchAndAddKADBacking() {
    super();
  }


  public void setDomainID(Integer domainID) {
    this.domainID = domainID;
  }

  public Integer getDomainID() {
    if (domainID == null) {
      setCurrentlySelectedDomainId();
    }
    return domainID;
  }
  

  public void onDomainSelected(ValueChangeEvent valueChangeEvent) {
    FacesContext context = FacesContext.getCurrentInstance();
    valueChangeEvent.getComponent().processUpdates(context);
    setCurrentlySelectedDomainId();
  }

  private void setCurrentlySelectedDomainId() {
    DCIteratorBinding iteratorBinding = ADFUtils.findIterator("DomainLookupVO1Iterator");
    DomainLookupVORowImpl row = (DomainLookupVORowImpl)iteratorBinding.getCurrentRow();
    if (row != null) {
      domainID = row.getDomainId();
      logger.info("Domain Id : " + domainID + " Selected");
      logger.info("Now Refreshing the iterator for showing dimensions pertinent to selected domain");
      DCIteratorBinding filteredDimensionsIterator =
        ADFUtils.findIterator("DimensionsFilteredByDomainVO1Iterator");
      DimensionsFilteredByDomainVOImpl dimensionsVO =
        (DimensionsFilteredByDomainVOImpl)filteredDimensionsIterator.getViewObject();
      dimensionsVO.setbDomainId(domainID);
      dimensionsVO.executeQuery();
    }
  }

  /**
   * Used to search for KADS
   * @param actionEvent
   */
  public void searchKAD(ActionEvent actionEvent) {
   boolean verifyEmpty=verifyEmpty(); 
   if(verifyEmpty){
       JSFUtils.addFacesErrorMessage("Kindly add a criteria");
       return;
     }
   boolean verify100 = verifyTotalWeight100();
   if (!verify100) {
     JSFUtils.addFacesErrorMessage("Total weight across all values must be 100");
     return;
   }
   searchKAD();
 }
  public void searchKAD() {
    OperationBinding opBinding=ADFUtils.findOperation("searchAndRankKad");
    Map paramsMap=opBinding.getParamsMap();
    paramsMap.put("businessProblem", businessProblem);
    opBinding.execute();
  }

  private boolean verifyEmpty() {
    DCIteratorBinding iteratorBinding = ADFUtils.findIterator("KADDimensionsAreaTempVO1Iterator");
    RowSetIterator rowSetIterator = null;
    try {
      rowSetIterator = iteratorBinding.getViewObject().createRowSetIterator(null);
      if(rowSetIterator.hasNext()){
        return false;
        }
      else{
        return true;
        }
    }
     finally {
      if (rowSetIterator != null) {
        rowSetIterator.closeRowSetIterator();
      }
    }
  }
  private boolean verifyTotalWeight100() {
    DCIteratorBinding iteratorBinding = ADFUtils.findIterator("KADDimensionsAreaTempVO1Iterator");
    RowSetIterator rowSetIterator = null;
    int weight = 0;
    try {
      rowSetIterator = iteratorBinding.getViewObject().createRowSetIterator(null);

      while (rowSetIterator.hasNext()) {
        KADDimensionsAreaTempVORowImpl row = (KADDimensionsAreaTempVORowImpl)rowSetIterator.next();
        if (row.getWeight() != null) {
          weight += row.getWeight();
        }
        if (weight > 100) {
          return false;
        }
      }
    } finally {
      if (rowSetIterator != null) {
        rowSetIterator.closeRowSetIterator();
      }
    }

    return weight == 100;
  }

  public String addKad() {
    OperationBinding addKAD=ADFUtils.findOperation("addKAD");
    Map operationMap=addKAD.getParamsMap();
    operationMap.put("domainId", domainID);
    addKAD.execute();
    if(addKAD.getErrors().isEmpty()){
      JSFUtils.addFacesInformationMessage("KAD was created successfully");
      }
    return "";
  }

  public void removeKADRow(ActionEvent actionEvent) {
    OperationBinding deleteBinding = ADFUtils.findOperation("Delete1");
    deleteBinding.execute();
  }

  public void addKADRowAndShowPopup(ActionEvent actionEvent) {
    OperationBinding createInsertKADTempBinding = ADFUtils.findOperation("CreateInsert1");
    createInsertKADTempBinding.execute();
    FacesContext context = FacesContext.getCurrentInstance();
    ExtendedRenderKitService erks =
      Service.getRenderKitService(context, ExtendedRenderKitService.class);
    //show popup
    erks.addScript(context,
                   "AdfPage.PAGE.findComponent('" + addPopup.getClientId() + "').show();");
  }
  public String deleteKAD() { 
    DCIteratorBinding itBinding= ADFUtils.findIterator("KadSearchTransVO1Iterator");
    KadSearchTransVORowImpl row= (KadSearchTransVORowImpl)itBinding.getViewObject().getCurrentRow();
    OperationBinding removeKADOp = ADFUtils.findOperation("removeKAD");
    Map removeOpMap = removeKADOp.getParamsMap();
    removeOpMap.put("kadId", row.getKadID());
    removeKADOp.execute();
    searchKAD();
    return "";
  }
  public void setAddPopup(RichPopup addPopup) {
    this.addPopup = addPopup;
  }

  public RichPopup getAddPopup() {
    return addPopup;
  }


 
 
  public void setBusinessProblem(Integer businessProblem) {
    this.businessProblem = businessProblem;
  }

  public Integer getBusinessProblem() {
    return businessProblem;
  }


  public void viewUsageStatistics(ActionEvent actionEvent) {
    Map pfsMap=AdfFacesContext.getCurrentInstance().getPageFlowScope();
    Integer kadId=(Integer)pfsMap.get("kadId");
    if(kadId!=null){
      DCIteratorBinding itBinding= ADFUtils.findIterator("SummaryUsageStatisticsVO1Iterator");
      SummaryUsageStatisticsVOImpl voImpl=(SummaryUsageStatisticsVOImpl)itBinding.getViewObject();
      voImpl.setbkadId(kadId);
      voImpl.executeQuery();
        FacesContext context = FacesContext.getCurrentInstance();
        ExtendedRenderKitService erks =
          Service.getRenderKitService(context, ExtendedRenderKitService.class);
        //show popup
        erks.addScript(context,
                       "AdfPage.PAGE.findComponent('" + summaryPopup.getClientId() + "').show();");
      }
  }

  public void setSummaryPopup(RichPopup summaryPopup) {
    this.summaryPopup = summaryPopup;
  }

  public RichPopup getSummaryPopup() {
    return summaryPopup;
  }

  public void linkClicked(ActionEvent actionEvent) {
   Map pfsMap= AdfFacesContext.getCurrentInstance().getPageFlowScope();
   Integer kadId=(Integer)pfsMap.get("kadId");
   String kadLink=(String)pfsMap.get("link");
   OperationBinding opBinding=ADFUtils.findOperation("incrementHitCount");
   Map paramsMap=opBinding.getParamsMap();
   paramsMap.put("kadId",kadId);
   opBinding.execute();
   Integer hitCount=(Integer) opBinding.getResult();
    DCIteratorBinding itBinding = ADFUtils.findIterator("KadSearchTransVO1Iterator");
    KadSearchTransVORowImpl row=(KadSearchTransVORowImpl) itBinding.getCurrentRow();
    row.setHitCounter(hitCount);
    ExtendedRenderKitService erks=Service.getRenderKitService(FacesContext.getCurrentInstance(), ExtendedRenderKitService.class);
    erks.addScript(FacesContext.getCurrentInstance(), "window.open(\""+kadLink+"\");");
  }

  public void onBusinessProblemSelected(ValueChangeEvent valueChangeEvent) {
    FacesContext context = FacesContext.getCurrentInstance();
    valueChangeEvent.getComponent().processUpdates(context);
    Integer value=(Integer) JSFUtils.resolveExpression("#{bindings.BusinessProblem_id}");
    logger.info("Business Problem selected "+value);
    if(valueChangeEvent.getNewValue()!=(new Integer(0))){ 
    businessProblem=new Integer(value);
    }
    else{
      businessProblem=null;
    }
  }

 

  public void addCriteriaRow(ActionEvent actionEvent) {
    OperationBinding opBinding=ADFUtils.findOperation("insertKADTempRow");
    opBinding.execute();
  }

  public void setRemainingWeight(Integer remainingWeight) {
    this.remainingWeight = remainingWeight;
  }

  public Integer getRemainingWeight() {
    return remainingWeight;
  }

  public void weightChanged(ValueChangeEvent valueChangeEvent) {
    FacesContext context = FacesContext.getCurrentInstance();
    valueChangeEvent.getComponent().processUpdates(context);
    processWeightChange();
   
  }

  public void removeCriteriaRow(ActionEvent actionEvent) {
    OperationBinding opBinding=ADFUtils.findOperation("Delete");
    opBinding.execute();
    processWeightChange();
  }

  private void processWeightChange() {
    DCIteratorBinding iteratorBinding = ADFUtils.findIterator("KADDimensionsAreaTempVO1Iterator");
    RowSetIterator rowSetIterator = null;
    int weight = 0;
    try {
      rowSetIterator = iteratorBinding.getViewObject().createRowSetIterator(null);

      while (rowSetIterator.hasNext()) {
        KADDimensionsAreaTempVORowImpl row = (KADDimensionsAreaTempVORowImpl)rowSetIterator.next();
        if (row.getWeight() != null) {
          weight += row.getWeight();
        }
      }
    } finally {
      if (rowSetIterator != null) {
        rowSetIterator.closeRowSetIterator();
      }
    }
    this.setRemainingWeight(100-weight);
  }


  public void setTable1(RichTable table1) {
    this.table1 = table1;
  }

  public RichTable getTable1() {
    return table1;
  }

  public void onDimensionSelected(ValueChangeEvent valueChangeEvent) {
   valueChangeEvent.getComponent().processUpdates(FacesContext.getCurrentInstance());
   AdfFacesContext.getCurrentInstance().addPartialTarget(table1);
   FacesContext.getCurrentInstance().renderResponse();
  }
}
