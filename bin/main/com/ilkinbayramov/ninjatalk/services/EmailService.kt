package com.ilkinbayramov.ninjatalk.services

import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.HtmlEmail

object EmailService {
    // TODO: GMAIL hesabı bilgilerinizi buraya girin (veya ortam değişkenlerinden alın)
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = 465 // or 587
    private const val SMTP_USERNAME = "ilkinbayramov066@gmail.com"
    private const val SMTP_PASSWORD = "rhzlwdiahzayuvbm"

    fun sendPasswordResetEmail(toEmail: String, resetLink: String) {
        println("=====================================================")
        println("TEST İÇİN ŞİFRE SIFIRLAMA LİNKİ:")
        println(resetLink)
        println("=====================================================")
        
        try {
            val email = HtmlEmail()
            email.hostName = SMTP_HOST
            email.setSmtpPort(SMTP_PORT)
            email.setAuthenticator(DefaultAuthenticator(SMTP_USERNAME, SMTP_PASSWORD))
            email.isSSLOnConnect = true
            email.setFrom(SMTP_USERNAME, "PeChat Support")
            email.subject = "PeChat - Reset Password"
            
            val htmlMsg = """
                <html>
                    <body>
                        <h2>Reset Password</h2>
                        <p>Click the link below to reset your password:</p>
                        <br/>
                        <a href="$resetLink">Reset My Password</a>
                        <br/><br/>
                        <p>If you didn't request this, please ignore this email.</p>
                    </body>
                </html>
            """.trimIndent()
            
            email.setHtmlMsg(htmlMsg)
            email.addTo(toEmail)
            
            email.send()
            println("Şifre sıfırlama e-postası başarıyla gönderildi: ${toEmail}")
        } catch (e: Exception) {
            println("E-posta gönderilirken hata oluştu: ${e.message}")
            e.printStackTrace()
        }
    }
}
