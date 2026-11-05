package cnu.ssd.pennywise.exception;

import cnu.ssd.pennywise.service.ExportService;

/**
 * Thrown by {@link ExportService} when finding any trouble
 * while exporting the selected transactions.
 */
public class ExportException extends RuntimeException {

    public ExportException(String message) {
        super(message);
    }
}
