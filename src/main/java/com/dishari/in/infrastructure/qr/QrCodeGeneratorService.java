package com.dishari.in.infrastructure.qr;

import com.dishari.in.exception.QrCodeGenerationException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Map;

@Service
@Slf4j
public class QrCodeGeneratorService {

    @Value("${app.qr.storage-path}")
    private String storagePath;

    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    private QrCodeStorageService qrCodeStorageService ;

    private static final int LOGO_SIZE_RATIO = 5;

    // ── Returns raw bytes — nothing saved to disk ────────────────
    public byte[] generateBytes(
            String content,
            int size,
            String fgColor,
            String bgColor,
            String logoUrl,
            String format) throws QrCodeGenerationException {
        try {
            BitMatrix matrix  = buildMatrix(content, size);
            BufferedImage img = renderImage(matrix, fgColor, bgColor, size);

            if (logoUrl != null && !logoUrl.isBlank()) {
                embedLogo(img, logoUrl, size);
            }

            return toBytes(img, format);


        } catch (WriterException | IOException ex) {
            throw new QrCodeGenerationException(
                    "Failed to generate QR bytes");
        }
    }

    // ── Returns file URL — saves to disk ────────────────────────
    public QrCodeGeneratedResult generateAndSave(
            String content,
            String slug,
            int size,
            String fgColor,
            String bgColor,
            String logoUrl,
            String format) throws QrCodeGenerationException {
        try {
            BitMatrix matrix  = buildMatrix(content, size);
            BufferedImage img = renderImage(matrix, fgColor, bgColor, size);

            if (logoUrl != null && !logoUrl.isBlank()) {
                embedLogo(img, logoUrl, size);
            }

            // Save to disk
            String fileName = generateFileName(slug, size, fgColor , bgColor , logoUrl , format) ;

            String filePath = qrCodeStorageService.saveQrCode(img, fileName, format);
            String fileUrl  = baseUrl + "/api/v1/urls/qr-code/file/" + fileName;

            log.info("QR saved: slug={} file={}", slug, fileName);

            return new QrCodeGeneratedResult(fileUrl, format, size);

        } catch (WriterException | IOException ex) {
            throw new QrCodeGenerationException(
                    "Failed to generate and save QR");
        }
    }
    public String generateFileName(String slug, int size, String fgColor , String bgColor , String logoUrl , String format) {
        return "qr_" + slug + "_" + size + "x" + size+ "_" + fgColor + "_" + bgColor + "_" + logoUrl + "." + format;
    }
    // ── Shared internals ─────────────────────────────────────────

    private BitMatrix buildMatrix(String content, int size)
            throws WriterException {
        Map<EncodeHintType, Object> hints =
                new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        return new QRCodeWriter()
                .encode(content, BarcodeFormat.QR_CODE, size, size, hints);
    }

    private BufferedImage renderImage(
            BitMatrix matrix, String fgHex, String bgHex, int size) {
        MatrixToImageConfig config = new MatrixToImageConfig(
                hexToArgb(fgHex), hexToArgb(bgHex));
        BufferedImage src = MatrixToImageWriter
                .toBufferedImage(matrix, config);

        BufferedImage argb = new BufferedImage(
                size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return argb;
    }

    private void embedLogo(
            BufferedImage qr, String logoUrl, int qrSize)
            throws IOException {
        BufferedImage logo = ImageIO.read(new URL(logoUrl));
        if (logo == null) return;

        int logoSize = qrSize / LOGO_SIZE_RATIO;
        int x = (qrSize - logoSize) / 2;
        int y = (qrSize - logoSize) / 2;

        Graphics2D g = qr.createGraphics();
        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRoundRect(x - 4, y - 4, logoSize + 8, logoSize + 8, 10, 10);
        g.drawImage(
                logo.getScaledInstance(logoSize, logoSize, Image.SCALE_SMOOTH),
                x, y, null);
        g.dispose();
    }

    private byte[] toBytes(BufferedImage image, String format)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format.toLowerCase(), baos);
        return baos.toByteArray();
    }

    private String saveToStorage(
            BufferedImage image, String fileName, String format)
            throws IOException {
        Path dir = Paths.get(storagePath);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        Path file = dir.resolve(fileName);
        ImageIO.write(image, format.toLowerCase(), file.toFile());
        return storagePath + "/" + fileName;
    }

    private int hexToArgb(String hex) {
        String cleaned = hex.startsWith("#") ? hex.substring(1) : hex;
        return (int)(0xFF000000 | Long.parseLong(cleaned, 16));
    }

    // ── Result for file-based generation ────────────────────────
    public record QrCodeGeneratedResult(
            String fileUrl,
            String format,
            int size
    ) {}


}