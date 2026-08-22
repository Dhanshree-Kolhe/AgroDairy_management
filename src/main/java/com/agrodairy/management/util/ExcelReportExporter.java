package com.agrodairy.management.util;

import com.agrodairy.management.model.Customer;
import com.agrodairy.management.model.MonthlyBill;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

public class ExcelReportExporter {

    public static void exportFinancialRecordsToExcel(List<MonthlyBill> bills, Map<Long, Customer> customerMap, File outputFile) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Monthly Financial Ledger");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Data Styles
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("₹#,##0.00"));

            String[] headers = {
                    "Bill Number", "Date", "Customer Code", "Customer Name", "Area",
                    "Milk Qty (L)", "Milk Charges", "Extra Orders", "Holiday Deductions",
                    "Gross Amount", "Previous Balance", "Net Payable", "Amount Paid", "Balance Due", "Status"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (MonthlyBill bill : bills) {
                Row row = sheet.createRow(rowIdx++);
                Customer customer = customerMap != null ? customerMap.get(bill.getCustomerId()) : null;

                row.createCell(0).setCellValue(bill.getBillNumber());
                row.createCell(1).setCellValue(bill.getBillDate() != null ? bill.getBillDate().toString() : "");
                row.createCell(2).setCellValue(customer != null ? customer.getCustomerCode() : "CUST-" + bill.getCustomerId());
                row.createCell(3).setCellValue(customer != null ? customer.getFullName() : "Customer #" + bill.getCustomerId());
                row.createCell(4).setCellValue(customer != null ? customer.getArea() : "");

                row.createCell(5).setCellValue(bill.getTotalMilkQuantity() != null ? bill.getTotalMilkQuantity().doubleValue() : 0.0);

                Cell c6 = row.createCell(6);
                c6.setCellValue(bill.getMilkCharges() != null ? bill.getMilkCharges().doubleValue() : 0.0);
                c6.setCellStyle(currencyStyle);

                Cell c7 = row.createCell(7);
                c7.setCellValue(bill.getExtraOrdersCharges() != null ? bill.getExtraOrdersCharges().doubleValue() : 0.0);
                c7.setCellStyle(currencyStyle);

                Cell c8 = row.createCell(8);
                c8.setCellValue(bill.getHolidayDeductions() != null ? bill.getHolidayDeductions().doubleValue() : 0.0);
                c8.setCellStyle(currencyStyle);

                Cell c9 = row.createCell(9);
                c9.setCellValue(bill.getGrossAmount() != null ? bill.getGrossAmount().doubleValue() : 0.0);
                c9.setCellStyle(currencyStyle);

                Cell c10 = row.createCell(10);
                c10.setCellValue(bill.getPreviousBalance() != null ? bill.getPreviousBalance().doubleValue() : 0.0);
                c10.setCellStyle(currencyStyle);

                Cell c11 = row.createCell(11);
                c11.setCellValue(bill.getNetPayable() != null ? bill.getNetPayable().doubleValue() : 0.0);
                c11.setCellStyle(currencyStyle);

                Cell c12 = row.createCell(12);
                c12.setCellValue(bill.getPaidAmount() != null ? bill.getPaidAmount().doubleValue() : 0.0);
                c12.setCellStyle(currencyStyle);

                Cell c13 = row.createCell(13);
                c13.setCellValue(bill.getBalanceDue() != null ? bill.getBalanceDue().doubleValue() : 0.0);
                c13.setCellStyle(currencyStyle);

                row.createCell(14).setCellValue(bill.getStatus() != null ? bill.getStatus().name() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
        }
    }
}
