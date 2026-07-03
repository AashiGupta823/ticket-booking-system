package com.tbs.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class QrCodeService {

    /**
     * Generates a PNG QR code that encodes just the booking reference.
     * The reference is looked up server-side at check-in time rather than
     * embedding any booking details directly in the code, so the QR image
     * itself carries no sensitive data.
     */
    public byte[] generateQrPng(String bookingReference, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(bookingReference, BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }
}
