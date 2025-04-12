package com.example.muzik.permisson1

import android.content.ContentResolver
import android.os.Bundle
import android.provider.ContactsContract
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.muzik.permisson1.adapters.ContactAdapter
import com.example.muzik.permisson1.databinding.ActivityMainBinding
import com.example.muzik.permisson1.models.Contact
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.PermissionListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val contactList = mutableListOf<Contact>() // Asl ro'yxat
    private val filteredContactList = mutableListOf<Contact>() // Filtrlangan ro'yxat
    private var currentSwipedViewHolder: RecyclerView.ViewHolder? = null
    private lateinit var adapter: ContactAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView sozlamasi
        binding.recy.layoutManager = LinearLayoutManager(this)
        adapter = ContactAdapter(filteredContactList) { deletedContact ->
            // O'chirilgan kontaktni asl ro'yxatdan ham o'chirish
            contactList.remove(deletedContact)
            filterContacts(binding.searchView.query.toString()) // Qidiruvni yangilash
        }
        binding.recy.adapter = adapter

        // Swipe mexanizmi (oldingi kod saqlanadi)
        val itemTouchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val binding = (viewHolder as ContactAdapter.ContactViewHolder).binding
                val swipeButtonsWidth = binding.swipeButtons.width.toFloat()

                if (currentSwipedViewHolder != null && currentSwipedViewHolder != viewHolder) {
                    val prevBinding =
                        (currentSwipedViewHolder as ContactAdapter.ContactViewHolder).binding
                    prevBinding.contactContent.animate().translationX(0f).setDuration(200).start()
                    prevBinding.swipeButtons.visibility = android.view.View.GONE
                    currentSwipedViewHolder = null
                }

                if (dX < 0) {
                    binding.swipeButtons.visibility = android.view.View.VISIBLE
                    val maxDx = -swipeButtonsWidth
                    binding.contactContent.translationX = if (isCurrentlyActive) {
                        dX.coerceAtLeast(maxDx)
                    } else {
                        maxDx
                    }
                } else if (dX > 0) {
                    binding.contactContent.translationX = if (isCurrentlyActive) {
                        dX.coerceAtMost(0f)
                    } else {
                        0f
                    }
                    if (dX >= swipeButtonsWidth / 2 && !isCurrentlyActive) {
                        binding.swipeButtons.visibility = android.view.View.GONE
                    }
                }

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.6f

            override fun getSwipeEscapeVelocity(defaultValue: Float): Float = defaultValue * 8

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                val binding = (viewHolder as ContactAdapter.ContactViewHolder).binding
                if (binding.contactContent.translationX < 0) {
                    binding.contactContent.translationX = -binding.swipeButtons.width.toFloat()
                    binding.swipeButtons.visibility = android.view.View.VISIBLE
                    currentSwipedViewHolder = viewHolder
                } else {
                    binding.contactContent.translationX = 0f
                    binding.swipeButtons.visibility = android.view.View.GONE
                    currentSwipedViewHolder = null
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recy)

        // SearchView sozlamasi
        setupSearchView()

        // Kontaktlarni yuklash
        checkPermissionAndLoadContacts()
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false // Submit bilan hech narsa qilmaymiz
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterContacts(newText ?: "")
                return true
            }
        })
    }

    private fun filterContacts(query: String) {
        filteredContactList.clear()
        if (query.isEmpty()) {
            filteredContactList.addAll(contactList) // Agar qidiruv bo'sh bo'lsa, barcha kontaktlarni ko'rsatamiz
        } else {
            val lowerCaseQuery = query.lowercase()
            filteredContactList.addAll(contactList.filter {
                it.name.lowercase().contains(lowerCaseQuery) ||
                        it.phoneNumber.lowercase().contains(lowerCaseQuery)
            })
        }
        adapter.notifyDataSetChanged()
    }

    private fun checkPermissionAndLoadContacts() {
        Dexter.withContext(this)
            .withPermission(android.Manifest.permission.READ_CONTACTS)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    loadContacts()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    binding.recy.adapter = ContactAdapter(emptyList<Contact>().toMutableList())
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: com.karumi.dexter.listener.PermissionRequest?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            }).check()
    }

    private fun loadContacts() {
        contactList.clear()
        filteredContactList.clear()
        val resolver: ContentResolver = contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                val phoneNumber = it.getString(numberIndex)
                contactList.add(Contact(name, phoneNumber))
            }
        }
        filteredContactList.addAll(contactList) // Dastlab barcha kontaktlarni filtrlangan ro'yxatga qo'shamiz
        adapter.notifyDataSetChanged()
    }
}