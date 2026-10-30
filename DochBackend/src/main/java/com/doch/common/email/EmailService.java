package com.doch.common.email;

import java.util.Map;

public interface EmailService {

    /**
     * Sends an HTML email using a Thymeleaf template.
     *
     * This method MUST NOT throw: it is called from inside @Transactional
     * business flows (staff/driver onboarding, seller registration, password
     * reset), and an escaping exception would roll back the whole business
     * transaction because an email could not be sent.
     *
     * @param to The recipient's email address.
     * @param subject The subject of the email.
     * @param templateName The name of the HTML template file (without the .html extension).
     * @param context A map of variables to be passed to the template.
     * @return true if the email was accepted by the provider, false if sending failed
     *         (the failure is logged; callers can adapt their response message).
     */
    boolean sendEmailFromTemplate(String to, String subject, String templateName, Map<String, Object> context);

}