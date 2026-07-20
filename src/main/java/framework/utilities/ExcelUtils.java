package framework.utilities;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal Excel (.xlsx) reader for test data.
 * Reads the first sheet, using row 0 as headers and row 1 as the data row.
 * Header names must match the target POJO's attribute names (e.g. {@code username}).
 */
public final class ExcelUtils {

    private static final Logger LOG = LoggerUtil.getLogger(ExcelUtils.class);
    private static final DataFormatter FORMATTER = new DataFormatter();

    private ExcelUtils() {
        // Prevent instantiation.
    }

    /**
     * @return the first data row as a {@code header -> value} map,
     *         or an empty map if the sheet has no data row.
     */
    public static Map<String, String> readFirstRow(String classpathResource) {
        try (InputStream input = ExcelUtils.class.getClassLoader().getResourceAsStream(classpathResource);
             Workbook workbook = new XSSFWorkbook(input)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            Row data = sheet.getRow(1);

            Map<String, String> values = new LinkedHashMap<>();
            if (header == null || data == null) {
                LOG.warn("Excel file '{}' has no data row", classpathResource);
                return values;
            }
            for (int column = 0; column < header.getLastCellNum(); column++) {
                String key = FORMATTER.formatCellValue(header.getCell(column)).trim();
                if (!key.isEmpty()) {
                    values.put(key, FORMATTER.formatCellValue(data.getCell(column)).trim());
                }
            }
            return values;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Excel resource: " + classpathResource, e);
        }
    }
}
