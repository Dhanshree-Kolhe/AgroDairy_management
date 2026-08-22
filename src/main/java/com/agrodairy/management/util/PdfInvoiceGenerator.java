package com.agrodairy.management.util;

import com.agrodairy.management.model.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormatSymbols;
import java.util.List;

public class PdfInvoiceGenerator {

    public static void generateMonthlyInvoicePdf(MonthlyBill bill, Customer customer, List<DeliveryRecord> deliveryRecords, List<ExtraOrder> extraOrders, List<Payment> payments, File outputFile) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, new FileOutputStream(outputFile));
        document.open();

        // Palette Colors
        Color primaryColor = new Color(37, 99, 235);    // Blue
        Color darkColor = new Color(30, 41, 59);       // Slate Dark
        Color lightBg = new Color(248, 250, 252);     // Light Gray/Blue

        // Fonts
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, primaryColor);
        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, darkColor);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, darkColor);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, darkColor);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

        // Header Section
        Paragraph header = new Paragraph("🥛 SMART AGRO DAIRY MANAGEMENT SYSTEM", titleFont);
        header.setAlignment(Element.ALIGN_LEFT);
        document.add(header);

        Paragraph subHeader = new Paragraph("Monthly Delivery & Billing Invoice", subTitleFont);
        subHeader.setSpacingAfter(10);
        document.add(subHeader);

        // Invoice Metadata Table
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setSpacingAfter(15);

        String monthName = new DateFormatSymbols().getMonths()[bill.getBillingMonth() - 1];

        PdfPCell c1 = new PdfPCell();
        c1.setBackgroundColor(lightBg);
        c1.setPadding(8);
        c1.addElement(new Paragraph("Customer Details:", boldFont));
        c1.addElement(new Paragraph("Name: " + customer.getFullName(), normalFont));
        c1.addElement(new Paragraph("Customer Code: " + customer.getCustomerCode(), normalFont));
        c1.addElement(new Paragraph("Phone: " + customer.getPhone(), normalFont));
        c1.addElement(new Paragraph("Address: " + customer.getAddress(), normalFont));
        metaTable.addCell(c1);

        PdfPCell c2 = new PdfPCell();
        c2.setBackgroundColor(lightBg);
        c2.setPadding(8);
        c2.addElement(new Paragraph("Bill Details:", boldFont));
        c2.addElement(new Paragraph("Bill Number: " + bill.getBillNumber(), normalFont));
        c2.addElement(new Paragraph("Billing Period: " + monthName + " " + bill.getBillingYear(), normalFont));
        c2.addElement(new Paragraph("Bill Date: " + bill.getBillDate(), normalFont));
        c2.addElement(new Paragraph("Status: " + bill.getStatus(), boldFont));
        metaTable.addCell(c2);

        document.add(metaTable);

        // Financial Summary Section
        Paragraph summaryHeading = new Paragraph("Invoice Financial Summary", subTitleFont);
        summaryHeading.setSpacingAfter(5);
        document.add(summaryHeading);

        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingAfter(15);

        addTableHeader(summaryTable, new String[]{"Description", "Quantity / Factor", "Rate Snapshot", "Amount (₹)"}, headerFont, primaryColor);

        addTableRow(summaryTable, "Total Milk Deliveries", bill.getTotalMilkQuantity() + " Liters", "-", "₹" + bill.getMilkCharges(), normalFont);
        addTableRow(summaryTable, "Extra Dairy Products", extraOrders != null ? extraOrders.size() + " Orders" : "0 Orders", "-", "₹" + bill.getExtraOrdersCharges(), normalFont);
        addTableRow(summaryTable, "Holiday Deductions", "-", "-", "- ₹" + bill.getHolidayDeductions(), normalFont);
        addTableRow(summaryTable, "Gross Monthly Amount", "-", "-", "₹" + bill.getGrossAmount(), boldFont);
        addTableRow(summaryTable, "Previous Balance", "-", "-", "₹" + bill.getPreviousBalance(), normalFont);
        addTableRow(summaryTable, "Net Payable Amount", "-", "-", "₹" + bill.getNetPayable(), boldFont);
        addTableRow(summaryTable, "Amount Paid", "-", "-", "₹" + bill.getPaidAmount(), normalFont);
        addTableRow(summaryTable, "Outstanding / Balance Due", "-", "-", "₹" + bill.getBalanceDue(), boldFont);

        document.add(summaryTable);

        // Detailed Milk Deliveries Table
        if (deliveryRecords != null && !deliveryRecords.isEmpty()) {
            Paragraph delHeading = new Paragraph("Daily Milk Delivery Log", subTitleFont);
            delHeading.setSpacingAfter(5);
            document.add(delHeading);

            PdfPTable delTable = new PdfPTable(5);
            delTable.setWidthPercentage(100);
            delTable.setSpacingAfter(15);

            addTableHeader(delTable, new String[]{"Date", "Shift", "Quantity (L)", "Rate / L", "Amount (₹)"}, headerFont, primaryColor);
            for (DeliveryRecord dr : deliveryRecords) {
                addTableRow(delTable, dr.getDeliveryDate().toString(), dr.getDeliveryShift().name(),
                        dr.getDeliveredQuantity().toString(), "₹" + dr.getRatePerLiter(), "₹" + dr.getTotalAmount(), normalFont);
            }
            document.add(delTable);
        }

        // Detailed Extra Orders Table
        if (extraOrders != null && !extraOrders.isEmpty()) {
            Paragraph extHeading = new Paragraph("Extra Dairy Products Delivered", subTitleFont);
            extHeading.setSpacingAfter(5);
            document.add(extHeading);

            PdfPTable extTable = new PdfPTable(6);
            extTable.setWidthPercentage(100);
            extTable.setSpacingAfter(15);

            addTableHeader(extTable, new String[]{"Date", "Product", "Unit", "Quantity", "Rate", "Total (₹)"}, headerFont, primaryColor);
            for (ExtraOrder eo : extraOrders) {
                String pName = eo.getProduct() != null ? eo.getProduct().getProductName() : "Product #" + eo.getProductId();
                String unit = eo.getProduct() != null ? eo.getProduct().getUnit().name() : "UNIT";
                addTableRow(extTable, eo.getDeliveryDate().toString(), pName, unit,
                        eo.getQuantity().toString(), "₹" + eo.getPriceSnapshot(), "₹" + eo.getTotalPrice(), normalFont);
            }
            document.add(extTable);
        }

        // Detailed Payments Table
        if (payments != null && !payments.isEmpty()) {
            Paragraph payHeading = new Paragraph("Payment Receipts", subTitleFont);
            payHeading.setSpacingAfter(5);
            document.add(payHeading);

            PdfPTable payTable = new PdfPTable(4);
            payTable.setWidthPercentage(100);
            payTable.setSpacingAfter(15);

            addTableHeader(payTable, new String[]{"Payment Date", "Mode", "Reference", "Amount Paid (₹)"}, headerFont, primaryColor);
            for (Payment p : payments) {
                addTableRow(payTable, p.getPaymentDate().toString(), p.getPaymentMode().name(),
                        p.getTransactionReference() != null ? p.getTransactionReference() : "-", "₹" + p.getAmountPaid(), normalFont);
            }
            document.add(payTable);
        }

        // Footer Section
        Paragraph footer = new Paragraph("Thank you for choosing Smart Agro Dairy Management! For support contact 9822011223.", normalFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(15);
        document.add(footer);

        document.close();
    }

    private static void addTableHeader(PdfPTable table, String[] headers, Font font, Color bgColor) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font));
            cell.setBackgroundColor(bgColor);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private static void addTableRow(PdfPTable table, String col1, String col2, String col3, String col4, Font font) {
        table.addCell(new PdfPCell(new Phrase(col1, font)));
        table.addCell(new PdfPCell(new Phrase(col2, font)));
        table.addCell(new PdfPCell(new Phrase(col3, font)));
        table.addCell(new PdfPCell(new Phrase(col4, font)));
    }

    private static void addTableRow(PdfPTable table, String col1, String col2, String col3, String col4, String col5, Font font) {
        table.addCell(new PdfPCell(new Phrase(col1, font)));
        table.addCell(new PdfPCell(new Phrase(col2, font)));
        table.addCell(new PdfPCell(new Phrase(col3, font)));
        table.addCell(new PdfPCell(new Phrase(col4, font)));
        table.addCell(new PdfPCell(new Phrase(col5, font)));
    }

    private static void addTableRow(PdfPTable table, String col1, String col2, String col3, String col4, String col5, String col6, Font font) {
        table.addCell(new PdfPCell(new Phrase(col1, font)));
        table.addCell(new PdfPCell(new Phrase(col2, font)));
        table.addCell(new PdfPCell(new Phrase(col3, font)));
        table.addCell(new PdfPCell(new Phrase(col4, font)));
        table.addCell(new PdfPCell(new Phrase(col5, font)));
        table.addCell(new PdfPCell(new Phrase(col6, font)));
    }
}
