package com.example.muzik.permisson1

import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.muzik.permisson1.databinding.ActivitySmsBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.PermissionListener

@Suppress("DEPRECATION")
class SmsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySmsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySmsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val contactName = intent.getStringExtra("contact_name") ?: "Noma'lum"
        val contactNumber = intent.getStringExtra("contact_number") ?: "Noma'lum"

        binding.tvSmsContactName.text = "$contactName"
        binding.tvSmsContactNumber.text = "$contactNumber"

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSendSms.setOnClickListener {
            val message = binding.etSmsMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                // SMS yuborish uchun ruxsatni tekshirish
                checkSmsPermissionAndSend(contactNumber, message)
            } else {
                Toast.makeText(this, "Xabar matnini kiriting", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkSmsPermissionAndSend(phoneNumber: String, message: String) {
        Dexter.withContext(this)
            .withPermission(android.Manifest.permission.SEND_SMS)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {

                    sendSms(phoneNumber, message)
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {

                    Toast.makeText(
                        this@SmsActivity,
                        "SMS yuborish uchun ruxsat kerak",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: com.karumi.dexter.listener.PermissionRequest?,
                    token: PermissionToken?
                ) {
                    // Ruxsat so‘rash dialogini ko‘rsatish
                    token?.continuePermissionRequest()
                }
            }).check()
    }

    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "SMS yuborildi", Toast.LENGTH_SHORT).show()
            binding.etSmsMessage.text.clear() // Matn maydonini tozalash
            finish() // Activity’ni yopish
        } catch (e: Exception) {
            Toast.makeText(this, "SMS yuborishda xatolik: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}