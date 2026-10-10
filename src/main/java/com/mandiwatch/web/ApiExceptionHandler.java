package com.mandiwatch.web;

import com.mandiwatch.service.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Error responses for the JSON API in the standard RFC 9457 "problem details" format:
 * {"type":..., "title":"Not Found", "status":404, "detail":"Commodity 'Xyz' not found", "instance":"/api/..."}
 * <p>
 * Extending ResponseEntityExceptionHandler gives ProblemDetail answers for all of Spring MVC's
 * own exceptions (missing parameter, wrong type, failed validation, ...) for free.
 * assignableTypes limits this advice to the API, so the HTML pages keep using error.html.
 */
@RestControllerAdvice(assignableTypes = PriceApiController.class)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Not Found");
        return problem;
    }
}
