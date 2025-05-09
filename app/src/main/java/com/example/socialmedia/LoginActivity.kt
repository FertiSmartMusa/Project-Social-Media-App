package com.example.socialmedia

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
            return
        }
        setContent {
            LoginScreen()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoggingIn by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 120.dp),
        contentAlignment = Alignment.TopStart
    ) {
//        Image(    //arka plan   rengi
//            painter = painterResource(id = R.drawable.back_ui),
//            contentDescription = "Background",
//            modifier = Modifier.fillMaxSize(),
//            contentScale = ContentScale.Crop
//        )
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.socialmedia),
                contentDescription = "Logo",
                modifier = Modifier.size(240.dp, 160.dp),
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email Icon") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = Color.White,
                    focusedBorderColor = MaterialTheme.colors.primary,
                    unfocusedBorderColor = Color.Gray,
                    textColor = Color.Black,
                    cursorColor = MaterialTheme.colors.primary
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password Icon") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = Color.White,
                    focusedBorderColor = MaterialTheme.colors.primary,
                    unfocusedBorderColor = Color.Gray,
                    textColor = Color.Black,
                    cursorColor = MaterialTheme.colors.primary
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(context, "Email is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (TextUtils.isEmpty(password)) {
                        Toast.makeText(context, "Password is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoggingIn = true
                    val progressDialog = ProgressDialog(context)
                    progressDialog.setTitle("Login")
                    progressDialog.setMessage("Logging in...")
                    progressDialog.setCanceledOnTouchOutside(false)
                    progressDialog.show()

                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            progressDialog.dismiss()
                            if (task.isSuccessful) {
                                val intent = Intent(context, MainActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                                (context as? LoginActivity)?.finish()
                            } else {
                                Toast.makeText(context, "Password or Email Invalid", Toast.LENGTH_LONG).show()
                                auth.signOut()
                            }
                            isLoggingIn = false
                        }
                },
                modifier = Modifier.width(150.dp)
            ) {
                Text("Login", fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Don't have an account? Sign Up",
                color = Color.Blue,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(context, SignUpActivity::class.java))
                }
            )
        }
    }

    if (isLoggingIn) {
        AlertDialog(
            onDismissRequest = { /* Boş bırakılabilir */ },
            title = { Text("Logging in") },
            text = { Text("Please wait...") },
            confirmButton = {}
        )
    }
}