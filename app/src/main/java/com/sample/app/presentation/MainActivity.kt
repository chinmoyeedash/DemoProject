package com.sample.app.presentation


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import com.sample.app.R
import com.sample.app.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding

  private val navHostFragment: NavHostFragment by lazy {
    (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
  }
  val navController: NavController by lazy {
    navHostFragment.navController
  }
  val navGraph: NavGraph by lazy {
    navController.navInflater.inflate(R.navigation.presentation_nav)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
  }

}