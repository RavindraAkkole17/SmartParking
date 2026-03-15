package com.example.smartparking.ui

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartparking.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.math.abs
import android.content.Intent
import android.net.Uri

class ParkingDetailsActivity : AppCompatActivity(), PaymentResultListener {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var rvSlots: RecyclerView
    private var slotAdapter: SlotAdapter? = null

    private lateinit var btnPreBook: Button
    private lateinit var btnSelectDate: Button
    private lateinit var tvSelectedDate: TextView
    private lateinit var btnNavigate: Button
    private lateinit var btnBack: ImageView
    private lateinit var progressLoading: ProgressBar

    private lateinit var tvParkingTitle: TextView
    private lateinit var tvOwner: TextView
    private lateinit var tvSlots: TextView

    private var adminLat: Double? = null
    private var adminLng: Double? = null

    private var selectedDate = ""
    private var selectedSlot: String? = null
    private var totalSlots = 0
    private lateinit var parkingId: String

    private val bookedSlots = mutableListOf<String>()

    // IP Webcam Snapshot URL
    private val IP_CAMERA_URL = "http://10.202.193.70:8080/shot.jpg"

    // ******** FINAL SLOT COORDINATES YOU PROVIDED ********
    private val slotCoordinates = mapOf(
        "S1" to Rect(292, 666, 310, 697),
        "S2" to Rect(591, 655, 605, 677),
        "S3" to Rect(885, 675, 899, 696),
        "S4" to Rect(1139, 687, 1162, 711),
        "S5" to Rect(1388, 694, 1406, 724),
        "S6" to Rect(1342, 79, 1367, 120),
        "S7" to Rect(1080, 73, 1102, 102),
        "S8" to Rect(862, 70, 870, 83),
        "S9" to Rect(664, 56, 682, 74),
        "S10" to Rect(400, 40, 410, 66)
    )

    // Detection parameters
    private val detectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val detectionIntervalMs = 1500L

    private var previousGrayPixels: IntArray? = null
    private var prevBitmapWidth = 0
    private var prevBitmapHeight = 0

    // Motion detection thresholds
    private val pixelDiffThreshold = 30
    private val percentChangedThreshold = 0.04

    // STATIC OBJECT THRESHOLD (YOU SELECTED 150)
    private val staticBrightnessThreshold = 150

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parking_details)

        parkingId = intent.getStringExtra("parkingId") ?: "PARKING_1"

        rvSlots = findViewById(R.id.rvSlots)
        rvSlots.layoutManager = GridLayoutManager(this, 5)

        btnPreBook = findViewById(R.id.btnPreBook)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        btnNavigate = findViewById(R.id.btnNavigate)
        btnBack = findViewById(R.id.btnBack)
        progressLoading = findViewById(R.id.progressLoading)

        tvParkingTitle = findViewById(R.id.tvParkingTitle)
        tvOwner = findViewById(R.id.tvOwner)
        tvSlots = findViewById(R.id.tvSlots)

        btnBack.setOnClickListener { finish() }
        btnNavigate.setOnClickListener { navigateToParking() }

        Checkout.preload(applicationContext)

        val slotNames = slotCoordinates.keys.toList()
        slotAdapter = SlotAdapter(slotNames, bookedSlots) { slot -> selectedSlot = slot }
        rvSlots.adapter = slotAdapter

        db.collection("parkings").document(parkingId)
            .collection("slotsLive")
            .addSnapshotListener { snapshot, _ ->
                val map = mutableMapOf<String, Boolean>()
                snapshot?.documents?.forEach { doc ->
                    map[doc.id] = doc.getBoolean("occupied") ?: false
                }
                runOnUiThread { slotAdapter?.updateLiveStatus(map) }
            }

        loadParkingDetails()
        loadBookedSlotsForToday()

        btnSelectDate.setOnClickListener { openDatePicker() }
        btnPreBook.setOnClickListener { bookSelectedSlot() }

        startRealtimeDetection()
    }

    // ---------------- Booking ----------------
    private fun loadBookedSlotsForToday() {
        val calendar = Calendar.getInstance()
        selectedDate = "${calendar.get(Calendar.YEAR)}-${"%02d".format(calendar.get(Calendar.MONTH) + 1)}-${"%02d".format(calendar.get(Calendar.DAY_OF_MONTH))}"
        loadBookedSlots(selectedDate)
        tvSelectedDate.text = selectedDate
    }

    private fun loadBookedSlots(date: String) {
        db.collection("parkings").document(parkingId)
            .collection("bookings").document(date)
            .get()
            .addOnSuccessListener { doc ->
                bookedSlots.clear()
                val list = doc.get("bookedSlots") as? List<String>
                if (list != null) bookedSlots.addAll(list)
                slotAdapter?.notifyDataSetChanged()
            }
    }

    private fun openDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = "$year-${"%02d".format(month + 1)}-${"%02d".format(day)}"
                tvSelectedDate.text = selectedDate
                loadBookedSlots(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun bookSelectedSlot() {
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Select a date first!", Toast.LENGTH_SHORT).show()
            return
        }
        selectedSlot?.let { startRazorpayPayment(it) }
            ?: Toast.makeText(this, "Please select a slot!", Toast.LENGTH_SHORT).show()
    }

    private fun startRazorpayPayment(slot: String) {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_R7ap4DMe5mgryT")

        try {
            val options = JSONObject()
            options.put("name", "Smart Parking")
            options.put("description", "Slot: $slot | Date: $selectedDate")
            options.put("currency", "INR")
            options.put("amount", 5000)

            val prefill = JSONObject()
            prefill.put("email", "test@example.com")
            prefill.put("contact", "9876543210")
            options.put("prefill", prefill)

            checkout.open(this, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPaymentSuccess(rzpId: String?) {
        selectedSlot?.let { saveBooking(it) }
    }

    override fun onPaymentError(code: Int, msg: String?) {
        Toast.makeText(this, "Payment Failed: $msg", Toast.LENGTH_SHORT).show()
    }

    private fun saveBooking(slot: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val bookingId = UUID.randomUUID().toString()
        val amount = 50

        val data = mapOf(
            "bookingId" to bookingId,
            "slot" to slot,
            "date" to selectedDate,
            "userId" to userId,
            "amount" to amount,
            "timestamp" to System.currentTimeMillis(),
            "parkingId" to parkingId
        )

        val adminRef = db.collection("parkings").document(parkingId)
            .collection("bookings").document(selectedDate)
            .collection("orders").document(bookingId)

        val userRef = db.collection("users").document(userId)
            .collection("bookings").document(bookingId)

        adminRef.set(data).addOnSuccessListener {
            db.collection("parkings").document(parkingId)
                .collection("bookings").document(selectedDate)
                .update("bookedSlots", FieldValue.arrayUnion(slot))

            userRef.set(data)

            Toast.makeText(this, "Slot Booked Successfully!", Toast.LENGTH_SHORT).show()
            selectedSlot = null
            loadBookedSlots(selectedDate)
        }
    }

    // ---------------- Detection Loop ----------------
    private fun startRealtimeDetection() {
        detectionScope.launch {
            while (isActive) {
                try {
                    runOnUiThread { progressLoading.visibility = View.VISIBLE }

                    val bitmap: Bitmap = try {
                        Picasso.get().load(IP_CAMERA_URL).get()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        runOnUiThread { progressLoading.visibility = View.GONE }
                        delay(detectionIntervalMs)
                        continue
                    }

                    val bmp = if (bitmap.config == Bitmap.Config.ARGB_8888)
                        bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)

                    val occupiedSet = processDetection(bmp)

                    val occupiedMap = slotCoordinates.keys.associateWith { occupiedSet.contains(it) }

                    for ((slotId, occupied) in occupiedMap) {
                        db.collection("parkings").document(parkingId)
                            .collection("slotsLive").document(slotId)
                            .set(mapOf("occupied" to occupied))
                    }

                    runOnUiThread {
                        slotAdapter?.updateLiveStatus(occupiedMap)
                        progressLoading.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread { progressLoading.visibility = View.GONE }
                }
                delay(detectionIntervalMs)
            }
        }
    }

    // ---------------- Motion + Static Detection ----------------
    private suspend fun processDetection(bmp: Bitmap): Set<String> = withContext(Dispatchers.Default) {

        val width = bmp.width
        val height = bmp.height

        val currentPixels = IntArray(width * height)
        bmp.getPixels(currentPixels, 0, width, 0, 0, width, height)

        val currentGray = IntArray(width * height)
        for (i in currentPixels.indices) {
            val p = currentPixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            currentGray[i] = ((0.299 * r) + (0.587 * g) + (0.114 * b)).toInt()
        }

        val prev = previousGrayPixels
        if (prev == null) {
            previousGrayPixels = currentGray
            prevBitmapWidth = width
            prevBitmapHeight = height
            return@withContext emptySet()
        }

        val occupiedSlots = mutableSetOf<String>()

        for ((slotId, rectOrig) in slotCoordinates) {

            val rect = Rect(rectOrig)

            val isMotion = detectMotion(prev, currentGray, width, rect)
            val isStatic = detectStaticObject(currentGray, width, height, rect)

            if (isMotion || isStatic) {
                occupiedSlots.add(slotId)
            }
        }

        previousGrayPixels = currentGray

        return@withContext occupiedSlots
    }

    // ---------------- MOTION DETECTION ----------------
    private fun detectMotion(prev: IntArray, current: IntArray, width: Int, rect: Rect): Boolean {

        var changedCount = 0
        var total = 0

        for (y in rect.top until rect.bottom) {
            val rowOffset = y * width
            for (x in rect.left until rect.right) {
                val idx = rowOffset + x
                total++
                if (abs(current[idx] - prev[idx]) > pixelDiffThreshold) {
                    changedCount++
                }
            }
        }

        if (total == 0) return false

        val changedPercent = changedCount.toDouble() / total.toDouble()
        return changedPercent >= percentChangedThreshold
    }

    // ---------------- STATIC DETECTION ----------------
    private fun detectStaticObject(grayPixels: IntArray, width: Int, height: Int, rect: Rect): Boolean {
        var sum = 0
        var count = 0

        for (y in rect.top until rect.bottom) {
            for (x in rect.left until rect.right) {
                val idx = y * width + x
                sum += grayPixels[idx]
                count++
            }
        }

        if (count == 0) return false

        val avgBrightness = sum / count

        return avgBrightness < staticBrightnessThreshold  // YOU SELECTED 150
    }

    // ---------------- Firestore Parking Info ----------------
    private fun loadParkingDetails() {
        db.collection("parkings").document(parkingId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    tvParkingTitle.text = doc.getString("name") ?: "-"
                    tvOwner.text = "Owner: ${doc.getString("owner") ?: "-"}"

                    totalSlots = doc.getLong("totalSlots")?.toInt()
                        ?: doc.getLong("slots_total")?.toInt() ?: 0

                    val freeSlots = doc.getLong("availableSlots")?.toInt()
                        ?: doc.getLong("slots_free")?.toInt() ?: 0

                    tvSlots.text = "Available Slots: $freeSlots / $totalSlots"

                    adminLat = doc.getDouble("lat")
                    adminLng = doc.getDouble("lng")
                }
            }
    }

    private fun navigateToParking() {
        if (adminLat == null || adminLng == null) {
            Toast.makeText(this, "Admin has not set location!", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = Uri.parse("google.navigation:q=${adminLat},${adminLng}&mode=d")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        if (intent.resolveActivity(packageManager) != null) startActivity(intent)
        else Toast.makeText(this, "Google Maps not installed!", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        detectionScope.coroutineContext.cancelChildren()
    }

    override fun onResume() {
        super.onResume()
        if (!detectionScope.isActive) startRealtimeDetection()
    }

    override fun onDestroy() {
        super.onDestroy()
        detectionScope.cancel()
    }
}
