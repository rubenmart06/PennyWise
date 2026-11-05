package cnu.ssd.pennywise.service;

import cnu.ssd.pennywise.exception.ExportException;
import com.opencsv.CSVWriter;
import cnu.ssd.pennywise.model.Transaction;
import cnu.ssd.pennywise.model.TransactionFilter;
import cnu.ssd.pennywise.model.TransactionType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * Business logic for exporting the transaction history: the
 * same filtered dataset {@code TransactionPanel} shows, as Excel or CSV bytes
 * handed straight back to the Controller to save (no file is written here).
 */
@Service
public class ExportService {

    private static final String[] HEADERS = {
            "Date", "Type", "Category", "Description", "Paid by", "Scope", "Currency", "Original amount", "Converted amount"
    };

    private final TransactionService transactionService;

    public ExportService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public byte[] exportToExcel(Long householdId, TransactionFilter filter) {
        List<Transaction> transactions = transactionService.searchTransactions(householdId, filter);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Transactions");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            int rowIndex = 1;
            for (Transaction t : transactions) {
                Row row = sheet.createRow(rowIndex++);
                writeRow(row, t);
            }
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new ExportException("Could not generate the Excel file.");
        }
    }

    public byte[] exportToCsv(Long householdId, TransactionFilter filter) {
        List<Transaction> transactions = transactionService.searchTransactions(householdId, filter);

        try (StringWriter stringWriter = new StringWriter(); CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            csvWriter.writeNext(HEADERS);
            for (Transaction t : transactions) {
                csvWriter.writeNext(toCsvRow(t));
            }
            csvWriter.flush();
            return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ExportException("Could not generate the CSV file.");
        }
    }

    private void writeRow(Row row, Transaction t) {
        String[] values = toCsvRow(t);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values[i]);
        }
    }

    private String[] toCsvRow(Transaction t) {
        String sign = t.getType() == TransactionType.EXPENSE ? "-" : "+";
        return new String[]{
                t.getDate().toString(),
                t.getType() == TransactionType.EXPENSE ? "Expense" : "Income",
                t.getCategory(),
                t.getDescription() == null ? "" : t.getDescription(),
                t.getPayerUsername(),
                t.isShared() ? "Shared" : "Individual",
                t.getCurrencyCode(),
                t.getOriginalAmount().toPlainString(),
                sign + t.getConvertedAmount().toPlainString()
        };
    }
}
