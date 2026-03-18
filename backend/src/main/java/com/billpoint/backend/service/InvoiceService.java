package com.billpoint.backend.service;

import com.billpoint.backend.model.Bill;
import com.billpoint.backend.model.BillItem;
import com.billpoint.backend.repository.BillItemRepository;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class InvoiceService {

    @Autowired
    private BillItemRepository billItemRepository;

    public byte[] generateInvoicePdf(Bill bill) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Shop Details
            document.add(new Paragraph(bill.getShop().getName()).setBold().setFontSize(20));
            document.add(new Paragraph(bill.getShop().getAddress() != null ? bill.getShop().getAddress() : "Address N/A"));
            document.add(new Paragraph("Phone: " + bill.getShop().getPhone()));
            document.add(new Paragraph("\n"));

            // Customer Details
            if (bill.getCustomer() != null) {
                document.add(new Paragraph("Customer: " + bill.getCustomer().getName()));
                document.add(new Paragraph("Phone: " + bill.getCustomer().getPhone()));
            } else {
                document.add(new Paragraph("Customer: Walk-in"));
            }
            document.add(new Paragraph("Date: " + bill.getCreatedAt()));
            document.add(new Paragraph("Invoice #: INV-" + bill.getId()));
            document.add(new Paragraph("\n"));

            // Items Table
            Table table = new Table(UnitValue.createPercentArray(new float[]{4, 2, 2, 2}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell("Item");
            table.addHeaderCell("Qty");
            table.addHeaderCell("Price");
            table.addHeaderCell("Total");

            List<BillItem> items = billItemRepository.findByBill_Id(bill.getId());
            for (BillItem item : items) {
                table.addCell(item.getProduct().getName());
                table.addCell(String.valueOf(item.getQuantity()));
                table.addCell("Rs. " + item.getPricePerUnit().toString());
                table.addCell("Rs. " + item.getTotalPrice().toString());
            }
            document.add(table);

            // Summary
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Subtotal: Rs. " + bill.getTotalAmount()));
            document.add(new Paragraph("Discount: Rs. " + bill.getDiscount()));
            document.add(new Paragraph("Grand Total: Rs. " + bill.getFinalAmount()).setBold().setFontSize(14));

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return baos.toByteArray();
    }
}
