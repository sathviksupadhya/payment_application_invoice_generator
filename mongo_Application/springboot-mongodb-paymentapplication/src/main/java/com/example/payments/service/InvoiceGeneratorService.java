package com.example.payments.service;

import com.example.payments.model.Payment;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class InvoiceGeneratorService {

    private static final String PDF_DIRECTORY = "invoices/";

    public void generateInvoice(Payment payment) {

        try {
            Path path = Paths.get(PDF_DIRECTORY);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String pdfFileName = PDF_DIRECTORY + "invoice_" + payment.getInvoicenumber() + ".pdf";

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(pdfFileName));
            document.open();

            document.add(new Paragraph("INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20)));
            document.add(new Paragraph(" "));
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            PdfPCell receiverCell = new PdfPCell(new Paragraph("Receiver Info: \n" + payment.getReceiverInfo()+"\nReceiver A/C: "+payment.getTargetBankAccount()));
            PdfPCell BuyerCell = new PdfPCell(new Paragraph("Buyer Info: \n" + payment.getBuyerInfo()+"\nBuyer A/C: "+payment.getSourceBankAccount()));
            receiverCell.setPadding(10);
            BuyerCell.setPadding(10);
            infoTable.addCell(receiverCell);
            infoTable.addCell(BuyerCell);
            document.add(infoTable);

            document.add(new Paragraph(" "));

            document.add(new Paragraph("P.O. Number: " + payment.getPonumber(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            document.add(new Paragraph(" "));

            PdfPTable itemTable = new PdfPTable(4);
            itemTable.setWidthPercentage(100);
            itemTable.addCell("Item Name");
            itemTable.addCell("Quantity");
            itemTable.addCell("Unit Price");
            itemTable.addCell("Total");

            double totalAmount = 0;
            for (Payment.Item item : payment.getItems()) {
                double itemTotal = item.getAmount() * item.getQuantity();
                totalAmount += itemTotal;

                itemTable.addCell(item.getItemName());
                itemTable.addCell(String.valueOf(item.getQuantity()));
                itemTable.addCell(String.valueOf(item.getAmount()));
                itemTable.addCell(String.valueOf(itemTotal));
            }

            PdfPCell emptyCell1 = new PdfPCell();
            PdfPCell emptyCell2 = new PdfPCell();
            emptyCell1.setBorder(Rectangle.NO_BORDER);
            emptyCell2.setBorder(Rectangle.NO_BORDER);
            itemTable.addCell(emptyCell1);
            itemTable.addCell(emptyCell2);
            itemTable.addCell(new PdfPCell(new Paragraph("Total")));
            itemTable.addCell(new PdfPCell(new Paragraph(String.valueOf(totalAmount))));
            document.add(itemTable);

            double tdsAmount = (totalAmount * payment.getTds()) / 100;
            double finalAmount = totalAmount - tdsAmount;
            PdfPTable tdsTable = new PdfPTable(4);
            tdsTable.setWidthPercentage(100);
            tdsTable.addCell(emptyCell1);
            tdsTable.addCell(emptyCell2);
            tdsTable.addCell("TDS (" + payment.getTds() + "%)");
            tdsTable.addCell(String.valueOf(tdsAmount));

            tdsTable.addCell(emptyCell1);
            tdsTable.addCell(emptyCell2);
            tdsTable.addCell("Final Total");
            tdsTable.addCell(String.valueOf(finalAmount));

            document.add(tdsTable);


            document.add(new Paragraph(" "));


            document.add(new Paragraph("Authorized Signature: ____________________", FontFactory.getFont(FontFactory.HELVETICA, 12)));


            document.close();
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }
    }
}