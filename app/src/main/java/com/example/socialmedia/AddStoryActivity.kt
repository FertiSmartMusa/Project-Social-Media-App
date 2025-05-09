package com.example.socialmedia

import android.app.ProgressDialog
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask

class AddStoryActivity : AppCompatActivity() {
    private var myUrl = ""
    private var imageUri: Uri? = null
    private var storageStoryRef: StorageReference? = null

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            if (uriContent != null) {
                imageUri = uriContent
                uploadStory()
            } else {
                Toast.makeText(this, "Failed to get cropped image URI.", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            val error = result.error
            Toast.makeText(this, "Crop failed: ${error?.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_story)
        storageStoryRef = FirebaseStorage.getInstance().reference.child("Story Pictures")

        startCrop()
    }

    private fun startCrop() {
        val cropOptions = CropImageOptions()
        cropOptions.guidelines = CropImageView.Guidelines.ON
        cropOptions.aspectRatioX = 1
        cropOptions.aspectRatioY = 1
        cropOptions.fixAspectRatio = true

        val contractOptions = CropImageContractOptions(null, cropOptions)
        cropImage.launch(contractOptions)
    }

    private fun uploadStory() {
        when {
            imageUri == null -> {
                Toast.makeText(this, "Please select Image", Toast.LENGTH_SHORT).show()
            }

            else -> {
                val progressDialog = ProgressDialog(this)
                progressDialog.setTitle("Adding Story")
                progressDialog.setMessage("Please wait while your story is added")
                progressDialog.show()

                val fileRef =
                    storageStoryRef!!.child(System.currentTimeMillis().toString() + ".jpg")

                val uploadTask: StorageTask<*>
                uploadTask = fileRef.putFile(imageUri!!)

                uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                            progressDialog.dismiss()
                        }
                    }

                    return@Continuation fileRef.downloadUrl
                })
                    .addOnCompleteListener(OnCompleteListener<Uri> { task ->
                        if (task.isSuccessful) {

                            val downloadUrl = task.result
                            myUrl = downloadUrl.toString()


                            val ref = FirebaseDatabase.getInstance().reference
                                .child("Story")
                                .child(FirebaseAuth.getInstance().currentUser!!.uid)

                            val storyId = (ref.push().key).toString()

                            val timeEnd =
                                System.currentTimeMillis() + 86400000 //864000 is the millisec conversion for 24hrs//The timeSpan to expire the story

                            val storymap = HashMap<String, Any>()

                            storymap["userid"] = FirebaseAuth.getInstance().currentUser!!.uid
                            storymap["timestart"] = ServerValue.TIMESTAMP
                            storymap["timeend"] = timeEnd
                            storymap["imageurl"] = myUrl
                            storymap["storyid"] = storyId

                            ref.child(storyId).updateChildren(storymap)

                            Toast.makeText(this, "Story Added!!", Toast.LENGTH_SHORT)
                                .show()

                            finish()


                            progressDialog.dismiss()

                        } else {
                            progressDialog.dismiss()
                        }
                    })

            }
        }
    }
}