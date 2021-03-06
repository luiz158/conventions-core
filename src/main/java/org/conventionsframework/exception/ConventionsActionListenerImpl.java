/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.conventionsframework.exception;

import org.conventionsframework.util.MessagesController;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.myfaces.application.ActionListenerImpl;

/**
 *
 * @author rmpestano Jun 21, 2011 11:21:08 PM
 * 
 * 
 */
public class ConventionsActionListenerImpl extends ActionListenerImpl implements Serializable {

    @Override
    @SuppressWarnings("CallToThreadDumpStack")
    public void processAction(ActionEvent event) {
        try {
            try {
                super.processAction(event);
            } catch (Throwable e) {
                throw ExceptionUtils.getRootCause(e);
            }
        } catch (BusinessException be) {
            MessagesController.addError(be.getDetail(), be.getSummary());
        } catch (Throwable ex) {//if its uncaught exception go to error page
            MessagesController.addFatal(ex.getMessage());
            FacesContext.getCurrentInstance().getExternalContext().log("UNEXPECTED ERROR:  " + ex.getMessage(), ex);
            handleFatalError(ex);
        }
    }

    /**
     * gather information about the exception and pass the info to errorBean so
     * the error details can be displayed to end user.
     *
     * @param ex
     */
    private void handleFatalError(final Throwable ex) {
        Throwable rootCause = ExceptionUtils.getRootCause(ex);
        String errorName = (rootCause == null) ? ex.getClass().getCanonicalName() : rootCause.getClass().getCanonicalName();
        String errorMessage = ExceptionUtils.getRootCauseMessage(ex);
        String stackTrace = ExceptionUtils.getStackTrace(rootCause == null ? ex : rootCause);
        /*
         * ELFlash.getFlash().put("errorName", errorName); /
         * ELFlash.getFlash().put("errorMessage", errorMessage);
         * ELFlash.getFlash().put("stackTrace", stackTrace); flash doesnt help
         * here, the ideia is to redirect to an error page
         */
        FacesContext context = FacesContext.getCurrentInstance();


        ErrorMBean errorMBean = context.getApplication().evaluateExpressionGet(context, "#{convErrorMBean}", ErrorMBean.class);
        errorMBean.setErrorMessage(errorMessage);
        errorMBean.setErrorName(errorName);
        errorMBean.setStacktrace(stackTrace);
        goToErrorPage();
    }

    /**
     * redirect to an error page('errorPage' navigation rule) with exception
     * information to display to the user in a friendly way
     */
    private void goToErrorPage() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            NavigationHandler navHandler = context.getApplication().getNavigationHandler();
            navHandler.handleNavigation(context, null, "errorPage");
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(ConventionsActionListenerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
