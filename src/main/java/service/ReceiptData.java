package service;

import java.time.LocalDate;

/**
 * Parsed information from a receipt OCR text.
 * Fields may be null when not confidently detected.
 */
public record ReceiptData(
        Double amount,
        LocalDate date,
        String title,
        String category,
        String paymentType
) {
}

