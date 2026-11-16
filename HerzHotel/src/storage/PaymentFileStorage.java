package storage;

import model.FileSavable;
import model.Payment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class PaymentFileStorage implements FileSavable {

    private final ArrayList<Payment> payments;

    public PaymentFileStorage() {
        payments = new ArrayList<>();
    }

    public void setPayments(List<Payment> paymentList) {
        payments.clear();

        if (paymentList != null) {
            payments.addAll(paymentList);
        }
    }

    public List<Payment> getPayments() {
        return new ArrayList<>(payments);
    }

    @Override
    public void saveToFile(String filePath) throws IOException {

        Path path = Path.of(filePath);

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        List<String> lines = new ArrayList<>();

        for (Payment payment : payments) {
            String line =
                    encode(payment.getPaymentId()) + "|"
                    + encode(payment.getBookingId()) + "|"
                    + payment.getAmount() + "|"
                    + encode(payment.getPaymentMethod()) + "|"
                    + payment.getPaymentDate() + "|"
                    + encode(payment.getPaymentStatus());

            lines.add(line);
        }

        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    @Override
    public void loadFromFile(String filePath) throws IOException {

        Path path = Path.of(filePath);

        payments.clear();

        if (!Files.exists(path)) {
            return;
        }

        List<String> lines = Files.readAllLines(
                path,
                StandardCharsets.UTF_8
        );

        for (String line : lines) {

            if (line.isBlank()) {
                continue;
            }

            String[] fields = line.split("\\|", -1);

            if (fields.length != 6) {
                throw new IOException(
                        "Invalid payment record in storage file."
                );
            }

            try {
                Payment payment = new Payment(
                        decode(fields[0]),
                        decode(fields[1]),
                        Double.parseDouble(fields[2]),
                        decode(fields[3]),
                        LocalDate.parse(fields[4]),
                        decode(fields[5])
                );

                payments.add(payment);

            } catch (IllegalArgumentException e) {
                throw new IOException(
                        "Unable to read a payment record.",
                        e
                );
            }
        }
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String decode(String value) {
        return new String(
                Base64.getDecoder().decode(value),
                StandardCharsets.UTF_8
        );
    }
}