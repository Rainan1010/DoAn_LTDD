package com.example.doan_ltdd

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var edtUser: EditText
    private lateinit var edtPass: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        database = AppDatabase.getDatabase(this)

        edtUser = findViewById(R.id.edtUser)
        edtPass = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)

        // Xử lý Đăng nhập
        btnLogin.setOnClickListener {
            val username = edtUser.text.toString().trim()
            val password = edtPass.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val user = database.userDao().login(username, password)
                if (user != null) {
                    Toast.makeText(this@LoginActivity, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    // TRUYỀN USERNAME SANG MAINACTIVITY
                    intent.putExtra("USERNAME_KEY", user.username)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Sai tên đăng nhập hoặc mật khẩu!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Xử lý Đăng ký (Hiện Popup)
        btnRegister.setOnClickListener {
            showRegisterDialog()
        }
    }

    private fun showRegisterDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_register, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        val etRegUser = dialogView.findViewById<EditText>(R.id.etRegUser)
        val etRegPass = dialogView.findViewById<EditText>(R.id.etRegPass)
        val etRegConfirm = dialogView.findViewById<EditText>(R.id.etRegConfirmPass)
        val btnRegSubmit = dialogView.findViewById<Button>(R.id.btnRegSubmit)

        btnRegSubmit.setOnClickListener {
            val user = etRegUser.text.toString().trim()
            val pass = etRegPass.text.toString().trim()
            val confirm = etRegConfirm.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirm) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val exists = database.userDao().checkUserExists(user)
                if (exists > 0) {
                    Toast.makeText(this@LoginActivity, "Tên đăng nhập đã tồn tại!", Toast.LENGTH_SHORT).show()
                } else {
                    val newUser = User(
                        username = user,
                        password = pass,
                        fullName = user, // Mặc định lấy username làm tên hiển thị
                        email = ""
                    )
                    database.userDao().registerUser(newUser)
                    Toast.makeText(this@LoginActivity, "Đăng ký thành công! Hãy đăng nhập.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }
}