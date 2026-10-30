package com.doch.common.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final TemplateEngine templateEngine;
    private final Resend resendClient;

    @Value("${application.email.from-name}")
    private String fromName;

    @Value("${application.email.from}")
    private String fromEmail;

    /**
     * Sends a templated email.
     *
     * IMPORTANT: this method is intentionally NON-THROWING. It is called from
     * inside @Transactional business flows (staff/driver onboarding, seller
     * registration, password reset). If any exception escapes here, the entire
     * business transaction rolls back — i.e. a driver fails to be created just
     * because an email could not be sent. Failures are logged and reported to
     * the caller via the boolean return value instead.
     */
    @Override
    public boolean sendEmailFromTemplate(String to, String subject, String templateName, Map<String, Object> context) {
        try {
            // Prepare the Thymeleaf context
            Context thymeleafContext = new Context();
            thymeleafContext.setVariables(context);

            // Process the HTML template to get the final HTML string
            String htmlBody = templateEngine.process("email/" + templateName + ".html", thymeleafContext);

            // Combine the name and email into the required format
            String formattedFrom = String.format("%s <%s>", fromName, fromEmail);

            // Build the Resend request
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(formattedFrom)
                    .to(to)
                    .subject(subject)
                    .html(htmlBody)
                    .build();

            // Send the email using the Resend SDK
            CreateEmailResponse data = resendClient.emails().send(params);

            log.info("Email sent via Resend. To: {}, Template: {}, ID: {}", to, templateName, data.getId());
            return true;

        } catch (ResendException e) {
            // API-level rejection (invalid key, unverified domain, bad recipient, etc.)
            log.error("Resend API rejected email to {} (template: {}): {}", to, templateName, e.getMessage());
            return false;
        } catch (Exception e) {
            // Catch-all: template processing errors, network failures, config issues.
            // Deliberately swallowed so the caller's @Transactional work still commits.
            log.error("Unexpected error sending email to {} (template: {})", to, templateName, e);
            return false;
        }
    }
}