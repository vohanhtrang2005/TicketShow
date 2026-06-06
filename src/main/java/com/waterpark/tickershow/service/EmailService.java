package com.waterpark.tickershow.service;

import com.waterpark.tickershow.entity.Booking;
import com.waterpark.tickershow.entity.Ticket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTicketsEmail(Booking booking, List<Ticket> tickets) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(booking.getReceiverEmail());
            message.setSubject("Ve dien tu TicketShow - Don #" + booking.getId());
            message.setText(buildEmailContent(booking, tickets));

            mailSender.send(message);
        } catch (MailException e) {
            System.out.println("Khong gui duoc email cho booking " + booking.getId());
            e.printStackTrace();
        }
    }

    private String buildEmailContent(Booking booking, List<Ticket> tickets) {
        StringBuilder content = new StringBuilder();

        content.append("Xin chao ").append(booking.getReceiverName()).append(",\n\n");
        content.append("Thanh toan cua ban da thanh cong.\n");
        content.append("Ma don hang: #").append(booking.getId()).append("\n");
        content.append("Tong tien: ").append(booking.getTotalAmount()).append(" VND\n\n");

        content.append("Danh sach ve cua ban:\n");

        for (int i = 0; i < tickets.size(); i++) {
            Ticket ticket = tickets.get(i);

            content.append(i + 1)
                    .append(". Ma ve: ")
                    .append(ticket.getTicketCode())
                    .append("\n");

            content.append("   QR content: ")
                    .append(ticket.getQrCode())
                    .append("\n");
        }

        content.append("\nVui long dua ma ve/QR khi check-in.\n");
        content.append("Cam on ban da su dung TicketShow.");

        return content.toString();
    }
}