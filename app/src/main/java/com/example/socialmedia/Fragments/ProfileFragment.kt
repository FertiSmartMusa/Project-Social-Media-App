package com.example.socialmedia.Fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.AccountSettings
import com.example.socialmedia.Adapter.MyPostAdapter
import com.example.socialmedia.Model.Post
import com.example.socialmedia.Model.User
import com.example.socialmedia.R
import com.example.socialmedia.ShowUsersActivity
import com.example.socialmedia.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var profileId: String
    private lateinit var firebaseUser: FirebaseUser

    private var postList: List<Post>? = null
    private var myPostAdapter: MyPostAdapter? = null

    private var postListSaved: List<Post>? = null
    private var myImagesAdapterSavedImg: MyPostAdapter? = null
    private var mySavedImg: List<String>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root
        // ... diğer kodlar ...
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        val pref = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)
        if (pref != null) {
            this.profileId = pref.getString("profileId", "none")!!
        }

        if (profileId == firebaseUser.uid) {
            binding.editProfileButton.text = "Edit Profile"
        } else if (profileId != firebaseUser.uid) {
            checkFollowOrFollowingButtonStatus()
        }
        //to call account profile setting activity
        binding.editProfileButton.setOnClickListener {
            val getButtontext = binding.editProfileButton.text.toString()
            when (getButtontext) {
                "Edit Profile" -> startActivity(
                    Intent(context, AccountSettings::class.java)
                )

                "Follow" -> {
                    firebaseUser.uid.let { userId ->
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(userId)
                            .child("Following").child(profileId)
                            .setValue(true)

                        pushNotification()
                    }

                    firebaseUser.uid.let { userId ->
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(profileId)
                            .child("Followers").child(userId)
                            .setValue(true)
                    }
                }

                "Following" -> {
                    firebaseUser.uid.let { userId ->
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(userId)
                            .child("Following").child(profileId)
                            .removeValue()
                    }

                    firebaseUser.uid.let { userId ->
                        FirebaseDatabase.getInstance().reference
                            .child("Follow").child(profileId)
                            .child("Followers").child(userId)
                            .removeValue()
                    }
                }
            }
        }

        binding.totalFollowers.setOnClickListener {
            Intent(context, ShowUsersActivity::class.java).apply {
                putExtra("id", profileId)
                putExtra("title", "followers")
                startActivity(this)
            }
        }
        binding.totalFollowing.setOnClickListener {
            Intent(context, ShowUsersActivity::class.java).apply {
                putExtra("id", profileId)
                putExtra("title", "following")
                startActivity(this)
            }
        }
        //to get own feeds
        var recyclerView: RecyclerView? = null
        recyclerView = binding.recyclerviewProfile
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
        postList = ArrayList()
        myPostAdapter = context?.let { MyPostAdapter(it, postList as ArrayList<Post>) }
        recyclerView.adapter = myPostAdapter

        //Adding recycler view for saved posts
        val recyclerViewSavedImages = binding.recyclerViewSavedPic
        recyclerViewSavedImages.setHasFixedSize(true)
        recyclerViewSavedImages.layoutManager = GridLayoutManager(context, 3)

        postListSaved = ArrayList()
        myImagesAdapterSavedImg = context?.let { MyPostAdapter(it, postListSaved as ArrayList<Post>) }
        recyclerViewSavedImages.adapter = myImagesAdapterSavedImg

        //Default
        recyclerViewSavedImages.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE

        //To view savedimages button function
        val uploadedImagesBtn = binding.postGrid
        uploadedImagesBtn.setOnClickListener {
            recyclerViewSavedImages.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }


        //To view uploadedimages button function
        val savedImagesBtn = binding.imagesSaveBtn
        savedImagesBtn.setOnClickListener {
            recyclerViewSavedImages.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }

        //to fill in data in profile page
        getFollowers()
        getFollowing()
        getNoofPosts()
        getUserInfo()
        myPosts()
        mySaves()
    }

    private fun mySaves() {
        mySavedImg = ArrayList()
        FirebaseDatabase.getInstance().reference.child("Saves").child(firebaseUser.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        (mySavedImg as ArrayList<String>).clear()
                        for (pO in snapshot.children) {
                            (mySavedImg as ArrayList<String>).add(pO.key!!)
                        }
                        readSavedImagesData() //Following is thr function to get the details of the saved posts
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                }
            })
    }

    private fun readSavedImagesData() {
        FirebaseDatabase.getInstance().reference.child("Posts")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(datasnapshot: DataSnapshot) {
                    if (datasnapshot.exists()) {
                        (postListSaved as ArrayList<Post>).clear()
                        for (snapshot in datasnapshot.children) {
                            val post = snapshot.getValue(Post::class.java)
                            for (key in mySavedImg!!) {
                                if (post!!.getPostId() == key) {
                                    (postListSaved as ArrayList<Post>).add(post)
                                }
                            }
                        }
                        myImagesAdapterSavedImg!!.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                }
            })
    }

    private fun checkFollowOrFollowingButtonStatus() {
        FirebaseDatabase.getInstance().reference
            .child("Follow").child(firebaseUser.uid)
            .child("Following")
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.child(profileId).exists()) {
                        binding.editProfileButton.text = "Following"
                    } else {
                        binding.editProfileButton.text = "Follow"
                    }
                }
            })
    }

    private fun getFollowers() {
        FirebaseDatabase.getInstance().reference
            .child("Follow").child(profileId)
            .child("Followers")
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        binding.totalFollowers.text = snapshot.childrenCount.toString()
                    }
                }
            })
    }

    private fun getFollowing() {
        FirebaseDatabase.getInstance().reference
            .child("Follow").child(profileId)
            .child("Following")
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        binding.totalFollowing.text = snapshot.childrenCount.toString()
                    }
                }
            })
    }

    private fun getNoofPosts() {
        FirebaseDatabase.getInstance().reference.child("Posts")
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                }

                override fun onDataChange(p0: DataSnapshot) {
                    var i = 0
                    for (snapshot in p0.children) {
                        val post = snapshot.getValue(Post::class.java)
                        if (post!!.getPublisher() == profileId) {
                            i++
                        }
                    }
                    binding.totalPosts.text = "$i"
                }
            })
    }

    private fun myPosts() {
        FirebaseDatabase.getInstance().reference.child("Posts")
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                }

                override fun onDataChange(p0: DataSnapshot) {
                    (postList as ArrayList<Post>).clear()
                    for (snapshot in p0.children) {
                        val post = snapshot.getValue(Post::class.java)
                        if (post!!.getPublisher() == profileId) {
                            (postList as ArrayList<Post>).add(post)
                        }
                    }
                    Collections.reverse(postList)
                    myPostAdapter!!.notifyDataSetChanged()
                }
            })
    }

    private fun pushNotification() {
        val ref = FirebaseDatabase.getInstance().reference.child("Notification").child(profileId)

        val notifyMap = HashMap<String, Any>()
        notifyMap["userid"] = FirebaseAuth.getInstance().currentUser!!.uid
        notifyMap["text"] = "➱Started following you "
        notifyMap["postid"] = ""
        notifyMap["ispost"] = true

        ref.push().setValue(notifyMap)
    }

    private fun getUserInfo() {
        FirebaseDatabase.getInstance().reference.child("Users").child(profileId)
            .addValueEventListener(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue<User>(User::class.java)
                        Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile)
                            .into(binding.profileImageProfile)
                        binding.profileToolbarUsername?.text = user.getUsername()
                        binding.fullnameInProfile?.text = user.getFullname()
                        binding.usernameInProfile?.text = user.getUsername()
                        binding.bioProfile?.text = user.getBio()
                    }
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Bağlayıcı nesnesini temizle
    }

    override fun onStop() {
        super.onStop()
        context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()?.apply {
            putString("profileId", firebaseUser.uid)
            apply()
        }
    }

    override fun onPause() {
        super.onPause()
        context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()?.apply {
            putString("profileId", firebaseUser.uid)
            apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)?.edit()?.apply {
            putString("profileId", firebaseUser.uid)
            apply()
        }
    }
}