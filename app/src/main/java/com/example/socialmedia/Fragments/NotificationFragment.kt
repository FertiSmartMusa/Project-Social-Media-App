package com.example.socialmedia.Fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.socialmedia.Model.Notification
import com.example.socialmedia.Model.Post
import com.example.socialmedia.Model.User
import com.example.socialmedia.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(navController: NavHostController) {
    val context = LocalContext.current
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    var notificationList by remember { mutableStateOf<List<Notification>>(emptyList()) }

    LaunchedEffect(Unit) {
        firebaseUser?.uid?.let { userId ->
            val notificationRef = FirebaseDatabase.getInstance().reference
                .child("Notification")
                .child(userId)

            notificationRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notifications = mutableListOf<Notification>()
                    for (childSnapshot in snapshot.children) {
                        childSnapshot.getValue(Notification::class.java)?.let {
                            notifications.add(it)
                        }
                    }
                    notificationList = notifications.reversed()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load notifications: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold)},
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(5.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            if (notificationList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No notifications yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            } else {
                items(notificationList) { notification ->
                    NotificationItem(notification = notification, onPostClick = { postId ->
                        val pref = context.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
                        pref.putString("postid", postId)
                        pref.apply()
                        (context as? FragmentActivity)?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.fragment_container, PostDetailFragment())?.commit()
                    }, onProfileClick = { userId ->
                        val pref = context.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit()
                        pref.putString("profileid", userId)
                        pref.apply()
                        (context as? FragmentActivity)?.supportFragmentManager?.beginTransaction()
                            ?.replace(R.id.fragment_container, ProfileFragment())?.commit()
                    })
                    Divider()
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                if (notification.getIsPost()) {
                    notification.getPostId()?.let { onPostClick(it) }
                } else {
                    onProfileClick(notification.getUserId())
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        var publisherUsername by remember { mutableStateOf("") }
        var publisherProfileImage by remember { mutableStateOf("") }
        var postImageUrl by remember { mutableStateOf<String?>(null) }
        var isPostVisible by remember { mutableStateOf(false) }

        LaunchedEffect(notification.getUserId()) {
            FirebaseDatabase.getInstance().reference.child("Users").child(notification.getUserId())
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        publisherUsername = user?.getUsername() ?: "Unknown"
                        publisherProfileImage = user?.getImage() ?: ""
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        if (notification.getIsPost()) {
            LaunchedEffect(notification.getPostId()) {
                FirebaseDatabase.getInstance().reference.child("Posts").child(notification.getPostId()!!)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val post = snapshot.getValue(Post::class.java)
                            postImageUrl = post?.getPostImage()
                            isPostVisible = true
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        } else {
            isPostVisible = false
            postImageUrl = null
        }

        AsyncImage(
            model = publisherProfileImage,
            contentDescription = "Profile Photo",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { onProfileClick(notification.getUserId()) },
            contentScale = ContentScale.Crop,
            placeholder = rememberVectorPainter(image = Icons.Outlined.Person)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = publisherUsername,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = notification.getText(),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isPostVisible && !postImageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = postImageUrl,
                contentDescription = "Post Photo",
                modifier = Modifier
                    .size(60.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        notification.getPostId()?.let { onPostClick(it) }
                    },
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(image = Icons.Outlined.Image)
            )
        }
    }
}

class NotificationFragment : Fragment() {
    private lateinit var navController: NavHostController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                navController = rememberNavController()
                NotificationScreen(navController = navController)
            }
        }
    }
}