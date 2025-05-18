package com.archemy.searchapp.view.beans;


import com.archemy.searchapp.model.lookups.AreaParentIdLookupVOImpl;
import com.archemy.searchapp.model.lookups.AreaParentIdLookupVORowImpl;
import com.archemy.searchapp.model.queries.AreasVORowImpl;

import com.blogspot.ramannanda.adf.utils.adffacesutils.ADFUtils;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

import oracle.adf.model.binding.DCIteratorBinding;

import oracle.jbo.ViewCriteria;


public class ManageAreasBacking {
  public ManageAreasBacking() {
  }

  public void onParentSelected(ValueChangeEvent valueChangeEvent) {
    FacesContext context = FacesContext.getCurrentInstance();
    valueChangeEvent.getComponent().processUpdates(context);
    int value = (Integer)valueChangeEvent.getNewValue();

    DCIteratorBinding itBinding = ADFUtils.findIterator("AreasVO2Iterator");
    AreasVORowImpl areaRow = (AreasVORowImpl)itBinding.getCurrentRow();
    
    Integer areaParentId=areaRow.getAreaParentId();
    DCIteratorBinding parentItBinding=ADFUtils.findIterator("AreaParentIdLookupVO2Iterator");
    AreaParentIdLookupVOImpl voImpl= (AreaParentIdLookupVOImpl)parentItBinding.getViewObject();
    ViewCriteria vc=voImpl.getViewCriteria("AreaParentIdLookupVOCriteria2");
    voImpl.executeEmptyRowSet();
    voImpl.setbAreaId(areaParentId);
    voImpl.setbDimensionId(areaRow.getDimensionId());
    voImpl.applyViewCriteria(vc);
    voImpl.executeQuery();
    AreaParentIdLookupVORowImpl row = 
//      (AreaParentIdLookupVORowImpl)voImpl.getRowSetIterator().next();
      (AreaParentIdLookupVORowImpl)areaRow.getAreaParentIdLookupVO1().getCurrentRow();
//    Integer areaDepthLevel = new Integer((Integer)row.getAttribute("AreaDepthLevel"));
    Integer areaDepthLevel = (Integer)row.getAttribute("AreaDepthLevel");

    if (areaDepthLevel == null) {
      areaDepthLevel = 0;
    } else {
      areaDepthLevel = areaDepthLevel + 1;
    }
    if (value == 0) {
      areaRow.setAreaDepthLevel(0);
    }
    else{
        areaRow.setAreaDepthLevel(areaDepthLevel);
      }
  }
}
