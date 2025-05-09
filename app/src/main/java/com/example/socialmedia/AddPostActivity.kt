package com.example.socialmedia

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.socialmedia.databinding.ActivityAddPostBinding
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask

class AddPostActivity : AppCompatActivity() {

    private var myUrl = ""
    private var imageUri: Uri? = null
    private var storagePostPictureRef: StorageReference? = null
    private lateinit var binding: ActivityAddPostBinding
    private lateinit var cropImageLauncher: ActivityResultLauncher<CropImageContractOptions>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storagePostPictureRef = FirebaseStorage.getInstance().reference.child("Post Picture")

        cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
            if (result.isSuccessful) {
                imageUri = result.uriContent
                binding.pictureToBePosted.setImageURI(imageUri)
            } else {
                val exception = result.error
                Toast.makeText(
                    this,
                    "Image Cropping failed: ${exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.dontPostPicture.setOnClickListener {
            val intent = Intent(this@AddPostActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        binding.postPicture.setOnClickListener {
            uploadPost()
        }

        binding.pictureToBePosted.setOnClickListener {
            startCrop()
        }
        startCrop()
    }

    private fun startCrop() {
        val cropOptions = CropImageOptions()
        cropOptions.guidelines = CropImageView.Guidelines.ON
        cropOptions.aspectRatioX = 1
        cropOptions.aspectRatioY = 1
        cropOptions.fixAspectRatio = true
        // baska özellikler
        // cropOptions.outputCompressFormat = Bitmap.CompressFormat.PNG
        // cropOptions.outputCompressQuality = 90
        // cropOptions.activityTitle = "Crop Image"

        val contractOptions = CropImageContractOptions(null, cropOptions)

        cropImageLauncher.launch(contractOptions)
    }

    private fun uploadPost() {
        val caption = binding.writePost.text.toString()
        when {
            imageUri == null -> Toast.makeText(
                this,
                "Please select image first.",
                Toast.LENGTH_LONG
            ).show()

            TextUtils.isEmpty(caption) -> Toast.makeText(
                this,
                "Please write caption",
                Toast.LENGTH_LONG
            ).show()

            else -> {
                val progressDialog = ProgressDialog(this)
                progressDialog.setTitle("Posting")
                progressDialog.setMessage("Please wait, we are posting..")
                progressDialog.setCanceledOnTouchOutside(false)
                progressDialog.show()

                val fileRef =
                    storagePostPictureRef!!.child(System.currentTimeMillis().toString() + ".jpg")

                var uploadTask: StorageTask<*>
                uploadTask = fileRef.putFile(imageUri!!)

                uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            progressDialog.dismiss()
                            throw it
                        }
                    }
                    return@Continuation fileRef.downloadUrl
                }).addOnCompleteListener(OnCompleteListener<Uri> { task ->
                    if (task.isSuccessful) {
                        val downloadUrl = task.result
                        myUrl = downloadUrl.toString()

                        val ref = FirebaseDatabase.getInstance().reference.child("Posts")
                        val postid = ref.push().key

                        if (postid != null) {
                            val postMap = HashMap<String, Any>()
                            postMap["postid"] = postid
                            postMap["caption"] = caption
                            postMap["publisher"] = FirebaseAuth.getInstance().currentUser!!.uid
                            postMap["postimage"] = myUrl

                            ref.child(postid).updateChildren(postMap)
                                .addOnCompleteListener { postTask ->
                                    if (postTask.isSuccessful) {
                                        val commentRef =
                                            FirebaseDatabase.getInstance().reference.child("Comment")
                                                .child(postid)
                                        val commentMap = HashMap<String, Any>()
                                        commentMap["publisher"] =
                                            FirebaseAuth.getInstance().currentUser!!.uid
                                        commentMap["comment"] = caption

                                        commentRef.push().setValue(commentMap)
                                            .addOnCompleteListener { commentTask ->
                                                progressDialog.dismiss()
                                                if (commentTask.isSuccessful) {
                                                    Toast.makeText(
                                                        this,
                                                        "Uploaded successfully",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    val intent = Intent(
                                                        this@AddPostActivity,
                                                        MainActivity::class.java
                                                    )
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    startActivity(intent)
                                                    finish()
                                                } else {
                                                    Toast.makeText(
                                                        this,
                                                        "Failed to add caption as comment: ${commentTask.exception?.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                    } else {
                                        progressDialog.dismiss()
                                        Toast.makeText(
                                            this,
                                            "Failed to upload post data: ${postTask.exception?.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        } else {
                            progressDialog.dismiss()
                            Toast.makeText(this, "Failed to generate post ID.", Toast.LENGTH_SHORT)
                                .show()
                        }

                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this,
                            "Failed to get download URL: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }).addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}