package com.example.socialmedia.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialmedia.Adapter.PostAdapter
import com.example.socialmedia.Adapter.StoryAdapter
import com.example.socialmedia.Model.Post
import com.example.socialmedia.Model.Story
import com.example.socialmedia.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private var postAdapter: PostAdapter? = null
    private var postList: MutableList<Post>? = null
    private var followingList: MutableList<String>? = null

    private var storyAdapter: StoryAdapter? = null
    private var storyList: MutableList<Story>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_home)
        val recyclerViewStory: RecyclerView = view.findViewById(R.id.recycler_view_story)

        setupRecyclerView(recyclerView, recyclerViewStory)
        checkFollowings()

        return view
    }

    private fun setupRecyclerView(recyclerView: RecyclerView, recyclerViewStory: RecyclerView) {
        recyclerView.layoutManager = LinearLayoutManager(context).apply {
            reverseLayout = true
            stackFromEnd = true
        }
        postList = ArrayList()
        postAdapter = context?.let { PostAdapter(it, postList as ArrayList<Post>) }
        recyclerView.adapter = postAdapter

        recyclerViewStory.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        storyList = ArrayList()
        storyAdapter = context?.let { StoryAdapter(it, storyList as ArrayList<Story>) }
        recyclerViewStory.adapter = storyAdapter
    }

    private fun checkFollowings() {
        followingList = ArrayList()

        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid

        val followingRef = FirebaseDatabase.getInstance().reference
            .child("Follow").child(currentUserId).child("Following")

        followingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                followingList?.clear()

                // Önce kendi UID'ini ekle
                followingList?.add(currentUserId)

                // Sonra takip ettiklerini ekle
                dataSnapshot.children.mapNotNullTo(followingList!!) { it.key }

                retrievePosts()
                retrieveStories()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun retrievePosts() {
        val postRef = FirebaseDatabase.getInstance().reference.child("Posts")

        postRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                postList?.clear()
                for (snapshot in dataSnapshot.children) {
                    val post = snapshot.getValue(Post::class.java)
                    if (post != null && followingList!!.contains(post.getPublisher())) {
                        postList!!.add(post)
                    }
                }
                postAdapter!!.notifyDataSetChanged()
            }
        })
    }

    private fun retrieveStories() {
        val timeCurrent = System.currentTimeMillis()
        FirebaseDatabase.getInstance().reference.child("Story")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    storyList?.clear()
                    storyList?.add(Story("", 0, 0, "", FirebaseAuth.getInstance().currentUser!!.uid))

                    for (id in followingList!!) {
                        var countStory = 0
                        val tempStories = ArrayList<Story>()

                        for (snapshot in dataSnapshot.child(id).children) {
                            val story = snapshot.getValue(Story::class.java)
                            if (story != null && timeCurrent > story.getTimeStart() && timeCurrent < story.getTimeEnd()) {
                                countStory++
                                tempStories.add(story)
                            }
                        }

                        if (countStory > 0) {
                            storyList?.addAll(tempStories)
                        }
                    }
                    storyAdapter?.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
