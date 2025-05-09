package com.example.socialmedia

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.socialmedia.Model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class AccountSettings : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            AccountSettingsScreen(navController = navController)
        }
    }
}

@Composable
fun AccountSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val storageRef = FirebaseStorage.getInstance().reference.child("Profile Pictures")
    val usersRef = FirebaseDatabase.getInstance().reference.child("Users").child(firebaseUser?.uid ?: "")

    var fullName by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileImageUrl by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        firebaseUser?.uid?.let {
            usersRef.get().addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(User::class.java)
                fullName = user?.getFullname() ?: ""
                userName = user?.getUsername() ?: ""
                bio = user?.getBio() ?: ""
                profileImageUrl = user?.getImage() ?: ""
            }
        }
    }

    // Galeri seçiminden sonra crop başlat
    val cropLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            profileImageUri = result.uriContent
        } else {
            Toast.makeText(context, "Image crop failed", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val cropOptions = CropImageContractOptions(
                uri,
                CropImageOptions().apply {
                    guidelines = CropImageView.Guidelines.ON
                    aspectRatioX = 1
                    aspectRatioY = 1
                }
            )
            cropLauncher.launch(cropOptions)
        }
    }

    fun uploadProfileImage() {
        if (profileImageUri == null) {
            Toast.makeText(context, "Select image", Toast.LENGTH_SHORT).show()
            return
        }

        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(userName)) {
            Toast.makeText(context, "Full name and username required", Toast.LENGTH_SHORT).show()
            return
        }

        isUploading = true
        val dialog = ProgressDialog(context)
        dialog.setTitle("Uploading...")
        dialog.show()

        val fileRef = storageRef.child("${firebaseUser!!.uid}.jpg")
        fileRef.putFile(profileImageUri!!)
            .continueWithTask { fileRef.downloadUrl }
            .addOnSuccessListener { downloadUri ->
                val userMap = mapOf(
                    "fullname" to fullName,
                    "username" to userName.lowercase(),
                    "bio" to bio,
                    "image" to downloadUri.toString()
                )
                usersRef.updateChildren(userMap).addOnCompleteListener {
                    dialog.dismiss()
                    isUploading = false
                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                    (context as? ComponentActivity)?.finish()
                }
            }.addOnFailureListener {
                dialog.dismiss()
                isUploading = false
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },

                navigationIcon = {
                    IconButton(onClick = {
                        (context as? ComponentActivity)?.finish()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = { uploadProfileImage() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                when {
                    profileImageUri != null -> AsyncImage(
                        model = profileImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.profile),
                        error = painterResource(id = R.drawable.profile)
                    )
                    profileImageUrl.isNotEmpty() -> AsyncImage(
                        model = profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.profile),
                        error = painterResource(id = R.drawable.profile)
                    )
                    else -> Image(
                        painter = painterResource(id = R.drawable.profile),
                        contentDescription = "Placeholder",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.weight(1f))

            //  Logout button
            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()

                    val intent = Intent(context, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                    (context as? ComponentActivity)?.finish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .width(10.dp)
            ) {
                Text("Logout")
            }

            // Delete Account
            Button(
                onClick = {
                    val user = FirebaseAuth.getInstance().currentUser
                    val uid = user?.uid

                    if (uid != null) {
                        // 1. Firebase Realtime Database üzerindeki veriyi sil
                        FirebaseDatabase.getInstance().reference.child("Users").child(uid).removeValue()
                            .addOnCompleteListener { dbTask ->
                                if (dbTask.isSuccessful) {
                                    // 2. Firebase Storage üzerindeki profil fotoğrafını sil
                                    FirebaseStorage.getInstance().reference.child("Profile Pictures").child("$uid.jpg")
                                        .delete()
                                        .addOnCompleteListener {
                                            // 3. Firebase Auth üzerinden hesabı sil
                                            user.delete()
                                                .addOnCompleteListener { authTask ->
                                                    if (authTask.isSuccessful) {
                                                        Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show()
                                                        val intent = Intent(context, LoginActivity::class.java)
                                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        context.startActivity(intent)
                                                        (context as? ComponentActivity)?.finish()
                                                    } else {
                                                        Toast.makeText(context, "Auth delete failed: ${authTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                        }
                                } else {
                                    Toast.makeText(context, "Database delete failed: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text("Delete Account", color = MaterialTheme.colors.onError)
            }

        }
    }
}
