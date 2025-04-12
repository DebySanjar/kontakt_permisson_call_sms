package com.example.muzik.permisson1.adapters

import android.content.Intent
import android.net.Uri
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.muzik.permisson1.R
import com.example.muzik.permisson1.SmsActivity
import com.example.muzik.permisson1.databinding.ItemContactBinding
import com.example.muzik.permisson1.models.Contact
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.PermissionListener

class ContactAdapter(
    private val contactList: MutableList<Contact>, // List o'rniga MutableList
    private val onContactDeleted: (Contact) -> Unit = {} // O'chirish uchun callback, default bo'sh
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactList[position]
        holder.bind(contact)
    }

    override fun getItemCount(): Int = contactList.size

    inner class ContactViewHolder(val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: Contact) {
            binding.tvContactName.text = contact.name
            binding.tvContactNumber.text = contact.phoneNumber

            binding.btnCall.setOnClickListener {
                Dexter.withContext(itemView.context)
                    .withPermission(android.Manifest.permission.CALL_PHONE)
                    .withListener(object : PermissionListener {
                        override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                            val intent = Intent(Intent.ACTION_CALL)
                            intent.data = Uri.parse("tel:${contact.phoneNumber}")
                            itemView.context.startActivity(intent)
                        }

                        override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                            Toast.makeText(
                                itemView.context,
                                "Qo‘ng‘iroq qilish uchun ruxsat kerak",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            permission: com.karumi.dexter.listener.PermissionRequest?,
                            token: PermissionToken?
                        ) {
                            token?.continuePermissionRequest()
                        }
                    }).check()
            }

            binding.btnSms.setOnClickListener {
                val intent = Intent(itemView.context, SmsActivity::class.java).apply {
                    putExtra("contact_name", contact.name)
                    putExtra("contact_number", contact.phoneNumber)
                }
                itemView.context.startActivity(intent)
            }

            binding.btnMore.setOnClickListener {
                showPopupMenu(it, contact)
            }
        }

        private fun showPopupMenu(view: View, contact: Contact) {
            val context = ContextThemeWrapper(view.context, R.style.PopupMenuStyle)
            val popup = PopupMenu(context, view)
            popup.menuInflater.inflate(R.menu.contact_options_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_share -> {
                        shareContact(contact)
                        true
                    }

                    R.id.action_copy -> {
                        copyContactNumber(contact.phoneNumber)
                        true
                    }

                    R.id.action_delete -> {
                        deleteContact(contact)
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }

        private fun shareContact(contact: Contact) {
            val shareText = "Ism: ${contact.name}\nRaqam: ${contact.phoneNumber}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            itemView.context.startActivity(Intent.createChooser(intent, "Kontaktni ulashish"))
        }

        private fun copyContactNumber(phoneNumber: String) {
            val clipboard =
                itemView.context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Telefon raqami", phoneNumber)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(itemView.context, "Raqam nusxalandi", Toast.LENGTH_SHORT).show()
        }

        private fun deleteContact(contact: Contact) {
            val position = contactList.indexOf(contact)
            if (position != -1) {
                contactList.removeAt(position) // Ro'yxatdan o'chirish
                notifyItemRemoved(position) // RecyclerView'ni yangilash
                onContactDeleted(contact) // Tashqariga xabar berish
                Toast.makeText(itemView.context, "${contact.name} o‘chirildi", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}