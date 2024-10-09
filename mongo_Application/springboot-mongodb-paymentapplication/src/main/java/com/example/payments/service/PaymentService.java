package com.example.payments.service;

import com.example.payments.dto.Paymentdto;
import com.example.payments.model.Payment;
import com.example.payments.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceGeneratorService invoiceGeneratorService ;

    public Payment initiatePayment(Paymentdto paymentdto) {
        Payment p=Payment.builder()
                .amount(paymentdto.getAmount())
                .currency(paymentdto.getCurrency())
                .username(paymentdto.getUsername())
                .ponumber(paymentdto.getPonumber())
                .invoicenumber(paymentdto.getInvoicenumber())
                .targetBankAccount(paymentdto.getTargetBankAccount())
                .tds(paymentdto.getTds())
                .sourceBankAccount(paymentdto.getSourceBankAccount())
                .status(paymentdto.getStatus())
                .paymentdate(paymentdto.getPaymentdate())
                .finalAmount((double)paymentdto.getAmount() - (((double)paymentdto.getTds() / 100) * (double)paymentdto.getAmount()))
                .buyerInfo(paymentdto.getBuyerInfo())
                .receiverInfo(paymentdto.getReceiverInfo())
                .items(Optional.ofNullable(paymentdto.getItems()).orElseGet(Collections::emptyList).stream()
                        .map(itemdto -> new Payment.Item(itemdto.getItemName(), itemdto.getQuantity(), itemdto.getAmount()))
                        .collect(Collectors.toList()))
                .build();
        Payment savedPayment = paymentRepository.save(p);

        // Generate PDF invoice after payment is saved
        invoiceGeneratorService.generateInvoice(savedPayment);

        return savedPayment;
    }
    // Method to initiate a list of payments
    public List<Payment> initiatePayments(List<Paymentdto> payments) {
        List<Payment> paymentList = payments.stream().map(payment -> Payment.builder()
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .username(payment.getUsername())
                .ponumber(payment.getPonumber())
                .invoicenumber(payment.getInvoicenumber())
                .targetBankAccount(payment.getTargetBankAccount())
                .tds(payment.getTds())
                .sourceBankAccount(payment.getSourceBankAccount())
                .status(payment.getStatus())
                .paymentdate(payment.getPaymentdate())
                .finalAmount((double)payment.getAmount() - (((double)payment.getTds() / 100) * (double)payment.getAmount()))
                .build()).collect(Collectors.toList());

        return paymentRepository.saveAll(paymentList);
    }
    // 1. Find pending payments
    public List<Payment> findPendingPayments() {
        return paymentRepository.findByStatus("PENDING");
    }

    // 2. Find total amount
    public Double getTotalAmount() {
        return paymentRepository.sumAllAmounts();
    }

    // 3. Find amount by invoice number
    public Double getAmountByInvoiceNumber(String invoiceNumber) {
        Payment payment = paymentRepository.findByInvoicenumber(invoiceNumber);
        return payment != null ? payment.getAmount() : 0.0;
    }

    // 4. Find complete and pending payments by payment date
    public Map<String, List<Payment>> getPaymentsByStatusAndDate(String paymentDate) {
        Map<String, List<Payment>> paymentsByStatus = new HashMap<>();
        paymentsByStatus.put("COMPLETED", paymentRepository.findByPaymentdateAndStatus(paymentDate, "PAID"));
        paymentsByStatus.put("PENDING", paymentRepository.findByPaymentdateAndStatus(paymentDate, "PENDING"));
        return paymentsByStatus;
    }

    // 5. Edit payment
    public Payment editPayment(String id, Paymentdto paymentdto) {
        Optional<Payment> optionalPayment = paymentRepository.findById(id);
        if (optionalPayment.isPresent()) {
            Payment payment = optionalPayment.get();
            payment.setAmount(paymentdto.getAmount());
            payment.setCurrency(paymentdto.getCurrency());
            payment.setUsername(paymentdto.getUsername());
            payment.setPonumber(paymentdto.getPonumber());
            payment.setInvoicenumber(paymentdto.getInvoicenumber());
            payment.setTargetBankAccount(paymentdto.getTargetBankAccount());
            payment.setSourceBankAccount(paymentdto.getSourceBankAccount());
            payment.setTds(paymentdto.getTds());
            payment.setStatus(paymentdto.getStatus());
            payment.setPaymentdate(paymentdto.getPaymentdate());
            payment.setFinalAmount(payment.getAmount() - ((payment.getTds() / 100) * payment.getAmount()));
            return paymentRepository.save(payment);
        }
        throw new RuntimeException("Payment not found");
    }

    // 6. Delete payment
    public void deletePayment(String id) {
        paymentRepository.deleteById(id);
    }

    // 7. get amount-tds=finalAmount
    public Double getAmountTdsFinalAmount(String invoiceNumber) {
        Payment payment = paymentRepository.findByInvoicenumber(invoiceNumber);

        return payment.getFinalAmount();
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
}
