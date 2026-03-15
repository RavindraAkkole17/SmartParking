package com.example.smartparking.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartparking.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nameEt = findViewById<EditText>(R.id.etName)
        val phoneEt = findViewById<EditText>(R.id.etPhone)
        val emailEt = findViewById<EditText>(R.id.etEmail)
        val passEt = findViewById<EditText>(R.id.etPassword)
        val signupBtn = findViewById<Button>(R.id.btnSignup)
        val loginBtn = findViewById<Button>(R.id.btnLogin)

        val roleGroup = findViewById<RadioGroup>(R.id.roleGroup)

        signupBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val phone = phoneEt.text.toString().trim()
            val email = emailEt.text.toString().trim()
            val pass = passEt.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // get selected role
            val selectedRoleId = roleGroup.checkedRadioButtonId
            if (selectedRoleId == -1) {
                Toast.makeText(this, "Select a role", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedRole = findViewById<RadioButton>(selectedRoleId)
            val role = selectedRole.text.toString().lowercase() // "admin" or "user"

            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    val uid = auth.currentUser!!.uid

                    val userData = mapOf(
                        "name" to name,
                        "phone" to phone,
                        "email" to email,
                        "role" to role
                    )

                    db.collection("users").document(uid)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()

                            // redirect by role
                            if (role == "admin") {
                                startActivity(Intent(this, AdminDashboardActivity::class.java))
                            } else {
                                startActivity(Intent(this, UserDashboardActivity::class.java))
                            }

                            finish()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Signup failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }

        loginBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
