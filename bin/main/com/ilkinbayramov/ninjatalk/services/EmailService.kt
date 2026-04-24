package com.ilkinbayramov.ninjatalk.services

import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.HtmlEmail

object EmailService {
    // TODO: GMAIL hesabı bilgilerinizi buraya girin (veya ortam değişkenlerinden alın)
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = 465 // or 587
    private const val SMTP_USERNAME = "your-email@gmail.com" 
    private const val SMTP_PASSWORD = "your-app-password" 

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
            email.setFrom(SMTP_USERNAME, "NinjaTalk Destek")
            email.subject = "NinjaTalk - Şifre Sıfırlama"
            
            val htmlMsg = """
                <html>
                    <body>
                        <h2>Şifrenizi Sıfırlayın</h2>
                        <p>Şifrenizi sıfırlamak için aşağıdaki linke tıklayın:</p>
                        <br/>
                        <a href="$resetLink">Şifremi Sıfırla</a>
                        <br/><br/>
                        <p>Eğer bu isteği siz yapmadıysanız, lütfen bu e-postayı dikkate almayın.</p>
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
