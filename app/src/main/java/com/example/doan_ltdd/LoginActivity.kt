package com.example.doan_ltdd

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    lateinit var edtUsername: EditText
    lateinit var edtPassword: EditText
    lateinit var btnLogin: Button
    lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        setControl()
        setEvent()
    }

    private fun setEvent() {
        val database = AppDatabase.getDatabase(this)
        val dao = database.userDao()

        btnLogin.setOnClickListener {
            val username = edtUsername.text.toString()
            val password = edtPassword.text.toString()
            lifecycleScope.launch {
                val user = dao.getLoginUser(username, password) // This is now a background operation

                // The UI update logic here is automatically dispatched back to the main thread
                if (user != null) {
                    // Đăng nhập thành công
                    Toast.makeText(this@LoginActivity, "Login successful.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    // Đăng nhập thất bại
                    Toast.makeText(this@LoginActivity, "Login failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnRegister.setOnClickListener {
            val username = edtUsername.text.toString()
            val password = edtPassword.text.toString()
            lifecycleScope.launch {
                dao.isUsernameExists(username)
                if (dao.isUsernameExists(username)) {
                    Toast.makeText(this@LoginActivity, "Người dùng đã tồn tại.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@LoginActivity, "Tạo tài khoản thành công: \tUsername: $username \tPassword: $password", Toast.LENGTH_SHORT).show()
                    dao.addUser(User(username, password, "", ""))
                }
            }
        }
    }

    private fun setControl() {
        edtUsername = findViewById(R.id.edtUser)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
    }
}