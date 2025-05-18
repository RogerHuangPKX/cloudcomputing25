package com.archemy.searchapp.view.navigation;


import java.io.Serializable;

import javax.faces.context.FacesContext;

import oracle.adf.controller.TaskFlowId;
import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.view.rich.component.rich.RichPopup;

import org.apache.myfaces.trinidad.render.ExtendedRenderKitService;
import org.apache.myfaces.trinidad.util.Service;


public class DynamicRegionBacking implements Serializable {
  @SuppressWarnings("compatibility:6095764882136145962")
  private static final long serialVersionUID = 1L;
  private String newTaskFlowId="/WEB-INF/taskflow/search-and-add-catalog-tf.xml#search-and-add-catalog-tf";
  private String taskFlowId =
    "/WEB-INF/taskflow/search-and-add-catalog-tf.xml#search-and-add-catalog-tf";
  private RichPopup popup;
  private String newTaskFlowIdUnChecked="";


  public DynamicRegionBacking() {
    super();
  }
  
    public String checkChanges() {
        DCBindingContainer dcBindingContainer=(DCBindingContainer)
        BindingContext.getCurrent().getCurrentBindingsEntry();
        if(dcBindingContainer.getDataControl().isTransactionModified()){
            /**
             *check placed here to confirm whether the oldtaskflow id and new taskflow id are same
             *and if they are it allows the change
             *
             */
        
            if(!taskFlowId.equals(newTaskFlowId)){
        FacesContext context = FacesContext.getCurrentInstance();
        ExtendedRenderKitService erks =
        Service.getRenderKitService(context, ExtendedRenderKitService.class);
        //show popup
        erks.addScript(context,"AdfPage.PAGE.findComponent('"+popup.getClientId()+"').show();"); 
            }
         }
        else{
            this.taskFlowId=newTaskFlowId;
            }
        
        return null;
    }
  /**
     * If the user insists on discarding changes rollback the transaction
     * @return
     */
    public String okAction() {
        DCBindingContainer dcBindingContainer=(DCBindingContainer)
        BindingContext.getCurrent().getCurrentBindingsEntry();
        dcBindingContainer.getDataControl().rollbackTransaction();
        this.taskFlowId=newTaskFlowId;
        return null;
    }

  public TaskFlowId getDynamicTaskFlowId() {
    return TaskFlowId.parse(taskFlowId);
  }

  public void setTaskFlowId(String taskFlowId) {
    this.newTaskFlowId=taskFlowId;
  }
  

  public String searchkadtaskflowdefinition() {
    taskFlowId =
        "/WEB-INF/taskflow/search-kad-task-flow-definition.xml#search-kad-task-flow-definition";
    return null;
  }

  public String searchandaddcatalogtf() {
    taskFlowId = "/WEB-INF/taskflow/search-and-add-catalog-tf.xml#search-and-add-catalog-tf";
    return null;
  }

  public String adddomainstaskflowdefinition() {
    taskFlowId =
        "/WEB-INF/taskflow/manage-domains-task-flow-definition.xml#add-domains-task-flow-definition";
    return null;
  }

  public String managedimensionstaskflowdefinition() {
    taskFlowId =
        "/WEB-INF/taskflow/manage-dimensions-task-flow-definition.xml#manage-dimensions-task-flow-definition";
    return null;
  }

  public String manageareastaskflowdefinition() {
    taskFlowId =
        "/WEB-INF/taskflow/manage-areas-task-flow-definition.xml#manage-areas-task-flow-definition";
    return null;
  }

  public String viewusagestatisticstf() {
    taskFlowId = "/WEB-INF/taskflow/view-usage-statistics-tf.xml#view-usage-statistics-tf";
    return null;
  }

  public String registerkadusagetaskflowdefinition() {
    taskFlowId =
        "/WEB-INF/taskflow/register-kad-usage-task-flow-definition.xml#register-kad-usage-task-flow-definition";
    return null;
  }

  public String recurringbusinessproblemtf() {
    taskFlowId = "/WEB-INF/taskflow/recurring-business-problem-tf.xml#recurring-business-problem-tf";
    return null;
  }

  public String editcustomerinfotf() {
    taskFlowId = "/WEB-INF/taskflow/edit-customer-info-tf.xml#edit-customer-info-tf";
    return null;
  }

  public String viewcustomerinfotaskflowdefinition() {
    taskFlowId = "/WEB-INF/taskflow/view-customer-info-task-flow-definition.xml#view-customer-info-task-flow-definition";
    return null;
  }

  public void setPopup(RichPopup popup) {
    this.popup = popup;
  }

  public RichPopup getPopup() {
    return popup;
  }

  public void setNewTaskFlowIdUnChecked(String newTaskFlowIdUnChecked) {
    this.taskFlowId = newTaskFlowIdUnChecked;
  }
}
