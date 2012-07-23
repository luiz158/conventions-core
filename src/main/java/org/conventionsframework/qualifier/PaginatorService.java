
package org.conventionsframework.qualifier;

import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.enterprise.util.Nonbinding;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD, METHOD, PARAMETER})
public @interface PaginatorService {
    
        @Nonbinding
	String name() default "";
        @Nonbinding
	Class entity() default byte.class;
        
        
}