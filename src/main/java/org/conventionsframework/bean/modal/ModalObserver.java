/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.conventionsframework.bean.modal;

import javax.enterprise.event.Observes;
import org.conventionsframework.event.ModalCallback;
import java.io.Serializable;

/**
 * 
 * @author Rafael M. Pestano Apr 17, 2011 9:29:55 AM
 */
public interface ModalObserver extends Serializable{
     
   void modalResponse(@Observes ModalCallback callback);
}
