package it.gov.pagopa.wispconverter.util;

import it.gov.pagopa.wispconverter.repository.model.enumz.OutcomeEnum;
import it.gov.pagopa.wispconverter.repository.model.enumz.WorkflowStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EndpointRETrace {
    WorkflowStatus status();

    OutcomeEnum outcomeOk() default OutcomeEnum.OK;

    OutcomeEnum outcomeError() default OutcomeEnum.KO;

    String businessProcess() default "not-configured";

    boolean reEnabled() default false;
}
