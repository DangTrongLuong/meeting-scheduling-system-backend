package com.meeting.schedule_a_meeting.service.users;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
public class TwoFactorAuthService {

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator(64);
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();

    // Tạo secret mới
    public String generateNewSecret() {
        return secretGenerator.generate();
    }

    // Tạo QR Code Data URI (đã xử lý exception)
    public String generateQrCodeDataUri(String secret, String email) throws QrGenerationException {
        QrData data = new QrData.Builder()
                .label(email)
                .issuer("CMC Meeting System")
                .secret(secret)
                .digits(6)
                .period(30)
                .algorithm(HashingAlgorithm.SHA1)
                .build();

        byte[] imageBytes = qrGenerator.generate(data);
        return getDataUriForImage(imageBytes, qrGenerator.getImageMimeType());
    }

    // Kiểm tra mã TOTP hợp lệ
    public boolean isValidCode(String secret, String code) {
        if (code == null || code.trim().isEmpty())
            return false;
        return verifier.isValidCode(secret, code.trim());
    }
}