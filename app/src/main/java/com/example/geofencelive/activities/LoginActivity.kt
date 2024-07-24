package com.example.geofencelive.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.geofencelive.Models.UserModal
import com.example.geofencelive.R
import com.example.geofencelive.databinding.ActivityLoginBinding
import com.google.firebase.firestore.FirebaseFirestore


class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"

    private var binding: ActivityLoginBinding?= null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding?.root)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginmain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding?.btnLogin?.setOnClickListener{
            userLogin()
        }



         val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.apply()

            val userEmail = sharedPreferences.getString("userEmail", null)
            if(!userEmail.isNullOrEmpty()){
                redirectToMain()
            }





    }

    private fun redirect(activity: String){
        val intent = when(activity){
            "Login" -> Intent(this,LoginActivity::class.java)
            "Main" -> Intent(this,MainActivity::class.java)
            else-> throw Exception("No Path Exists")
        }
        startActivity(intent)
        finish()
    }

    private fun redirectToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun userLogin() {

        Toast.makeText(this, "Method called", Toast.LENGTH_LONG).show()
        val email = binding?.etEmailAddress?.text.toString()
        val password = binding?.etPassword?.text.toString()


        getUserDataByEmail(email) { user ->
            if (user != null && user.password == password) {
                Log.d(TAG, "${user.username}, ${user.deviceIdentifier}")

                val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("userEmail", user.email)
                editor.putString("userName", user.username) // Save username if needed
                editor.putString("userDeviceIdentifier", user.deviceIdentifier) // Save device identifier if needed
                editor.apply()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Wrong email or password", Toast.LENGTH_SHORT).show()
            }

        }

    }

    fun getUserDataByEmail(email: String, callback: (UserModal?) -> Unit) {

        val db = FirebaseFirestore.getInstance()
        val userCollection = db.collection("users")
        userCollection.whereEqualTo("email", email).get()
            .addOnSuccessListener { documents ->
                if (documents != null && !documents.isEmpty) {
                    val user = documents.documents[0].toObject(UserModal::class.java)
                    callback(user)

                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.w("FirestoreHelper", "Error getting documents: ", exception)
                callback(null)
            }
    }
}