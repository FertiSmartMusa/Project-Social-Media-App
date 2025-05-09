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
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SignUpScreen()
        }
    }
}

@Composable
fun SignUpScreen() {
    var fullName by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSigningUp by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val database = remember { FirebaseDatabase.getInstance().reference.child("Users") }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
//        Image(
//            painter = painterResource(id = R.drawable.back_ui),
//            contentDescription = "Arka Plan",
//            modifier = Modifier.fillMaxSize(),
//            contentScale = ContentScale.Crop
//        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(top = 140.dp)
                .padding(horizontal = 20.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.socialmedia),
                contentDescription = "Logo",
                modifier = Modifier.size(220.dp, 130.dp),
                //colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Person Icon") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = Color.White.copy(alpha = 0.8f),
                    focusedBorderColor = MaterialTheme.colors.primary,
                    unfocusedBorderColor = Color.Gray,
                    textColor = Color.Black,
                    cursorColor = MaterialTheme.colors.primary
                )
            )

            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Filled.AccountBox, contentDescription = "Account Icon") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = Color.White,
                    focusedBorderColor = MaterialTheme.colors.primary,
                    unfocusedBorderColor = Color.Gray,
                    textColor = Color.Black,
                    cursorColor = MaterialTheme.colors.primary
                )
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email Icon") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = Color.White.copy(alpha = 0.8f),
                    focusedBorderColor = MaterialTheme.colors.primary,
                    unfocusedBorderColor = Color.Gray,
                    textColor = Color.Black,
                    cursorColor = MaterialTheme.colors.primary
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Password Icon") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = Color.White.copy(alpha = 0.8f),
                    focusedBorderColor = MaterialTheme.colors.primary,
                    unfocusedBorderColor = Color.Gray,
                    textColor = Color.Black,
                    cursorColor = MaterialTheme.colors.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    when {
                        TextUtils.isEmpty(fullName) -> Toast.makeText(context, "Name is required", Toast.LENGTH_SHORT).show()
                        TextUtils.isEmpty(userName) -> Toast.makeText(context, "Username is required", Toast.LENGTH_SHORT).show()
                        TextUtils.isEmpty(email) -> Toast.makeText(context, "Email is required", Toast.LENGTH_SHORT).show()
                        TextUtils.isEmpty(password) -> Toast.makeText(context, "Password is required", Toast.LENGTH_SHORT).show()
                        else -> createAccount(fullName, userName, email, password, auth, database, context)
                    }
                },
                modifier = Modifier.width(150.dp)
            ) {
                Text("Sign Up", fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Already have an account? Login",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                    (context as? SignUpActivity)?.finish()
                }
            )
        }
    }

    if (isSigningUp) {
        AlertDialog(
            onDismissRequest = { /* Boş bırakılabilir */ },
            title = { Text("Signing Up") },
            text = { Text("Please wait...") },
            confirmButton = {}
        )
    }
}

private fun createAccount(
    fullName: String,
    userName: String,
    email: String,
    password: String,
    auth: FirebaseAuth,
    database: DatabaseReference,
    context: android.content.Context
) {
    val progressDialog = ProgressDialog(context)
    progressDialog.setTitle("SignUp")
    progressDialog.setMessage("Please wait...")
    progressDialog.setCanceledOnTouchOutside(false)
    progressDialog.show()

    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                saveUserInfo(fullName, userName, email, database, progressDialog, context)
            } else {
                val message = task.exception?.toString() ?: "An error occurred."
                Toast.makeText(context, "Error : $message", Toast.LENGTH_LONG).show()
                auth.signOut()
                progressDialog.dismiss()
            }
        }
}

private fun saveUserInfo(
    fullName: String,
    userName: String,
    email: String,
    database: DatabaseReference,
    progressDialog: ProgressDialog,
    context: android.content.Context
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    if (currentUserId != null) {
        val userMap = HashMap<String, Any>()
        userMap["uid"] = currentUserId
        userMap["fullname"] = fullName
        userMap["username"] = userName.toLowerCase()
        userMap["email"] = email
        userMap["bio"] = "My personal social media account."
        userMap["image"] = "profile.png"

        database.child(currentUserId).setValue(userMap)
            .addOnCompleteListener { task ->
                progressDialog.dismiss()
                if (task.isSuccessful) {
                    Toast.makeText(context, "Account has been created", Toast.LENGTH_SHORT).show()

                    FirebaseDatabase.getInstance().reference
                        .child("Follow").child(currentUserId)
                        .child("Following").child(currentUserId)
                        .setValue(true)

                    val intent = Intent(context, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    (context as? SignUpActivity)?.finish()
                } else {
                    val message = task.exception?.toString() ?: "Failed to save user info."
                    Toast.makeText(context, "Error : $message", Toast.LENGTH_LONG).show()
                    FirebaseAuth.getInstance().signOut()
                }
            }
    } else {
        progressDialog.dismiss()
        Toast.makeText(context, "Failed to get current user ID.", Toast.LENGTH_LONG).show()
    }
}