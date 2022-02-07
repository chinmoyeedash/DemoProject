package com.sample.app.presentation.ui.home

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.sample.app.R
import com.sample.app.databinding.FragmentHomeListBinding
import com.sample.app.domain.models.IPagingModel
import com.sample.app.presentation.extension.GlideUtils
import com.sample.app.presentation.extension.hide
import com.sample.app.presentation.extension.observe
import com.sample.app.presentation.extension.show
import com.sample.app.presentation.ui.home.memes.MemesAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * A fragment representing a list of Items.
 */
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class HomeFragment : Fragment(), MemesAdapter.ItemListener {

  private var adapter: MemesAdapter? = null
  private lateinit var binding: FragmentHomeListBinding
  private val viewModel: MemesViewModel by viewModels()

  private var currentAnimator: Animator? = null
  private var currentThumbView: View? = null
  private lateinit var startBounds: RectF
  private var startScale: Float = 0f

  // The system "short" animation time duration, in milliseconds. This
  // duration is ideal for subtle animations or animations that occur
  // very frequently.
  private var shortAnimationDuration: Int = 5

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home_list, container, false)
    binding.lifecycleOwner = viewLifecycleOwner
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initAdapter()
    viewModel.getMemes()

    observe(viewModel.playersData) { list ->
      list?.let {
        adapter?.updateItems(list)
      }
    }


    observe(viewModel.showError) { error ->
      error?.let {
        Toast.makeText(
          requireContext(),
          it,
          Toast.LENGTH_SHORT
        ).show()
      }
    }

    observe(viewModel.showProgressbar) { show ->
      binding.progressBar.visibility = if (show == true) VISIBLE else GONE
    }

    activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if(binding.expandedImage.isVisible) hideExpandedImage()
        else {
          this.isEnabled = false
          activity?.onBackPressed()
        }
      }
    })
  }

  private fun initAdapter() {
    adapter = MemesAdapter()
    adapter?.setListener(this)
    binding.list.adapter = adapter
    //saves scroll position on configuration change
    binding.list.adapter?.stateRestorationPolicy =
      RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

  }

  override fun onDestroyView() {
    super.onDestroyView()
    adapter = null
    currentThumbView = null
    currentAnimator = null
  }


  companion object {
    const val TAG = "HomeFragment"


  }

  override fun onMemeClick(view: ImageView, item: IPagingModel) {
    zoomImageFromThumb(view, item.url)
  }

  private fun zoomImageFromThumb(thumbView: View, url: String?) {
    currentThumbView = thumbView
    // If there's an animation in progress, cancel it
    // immediately and proceed with this one.
    currentAnimator?.cancel()

    // Load the high-resolution "zoomed-in" image.
    GlideUtils.load(binding.expandedImage, imageUrl = url)

    // Calculate the starting and ending bounds for the zoomed-in image.
    // This step involves lots of math. Yay, math.
    val startBoundsInt = Rect()
    val finalBoundsInt = Rect()
    val globalOffset = Point()

    // The start bounds are the global visible rectangle of the thumbnail,
    // and the final bounds are the global visible rectangle of the container
    // view. Also set the container view's offset as the origin for the
    // bounds, since that's the origin for the positioning animation
    // properties (X, Y).
    thumbView.getGlobalVisibleRect(startBoundsInt)
    binding.container
      .getGlobalVisibleRect(finalBoundsInt, globalOffset)
    startBoundsInt.offset(-globalOffset.x, -globalOffset.y)
    finalBoundsInt.offset(-globalOffset.x, -globalOffset.y)

    startBounds = RectF(startBoundsInt)
    val finalBounds = RectF(finalBoundsInt)

    // Adjust the start bounds to be the same aspect ratio as the final
    // bounds using the "center crop" technique. This prevents undesirable
    // stretching during the animation. Also calculate the start scaling
    // factor (the end scaling factor is always 1.0).
    if ((finalBounds.width() / finalBounds.height() > startBounds.width() / startBounds.height())) {
      // Extend start bounds horizontally
      startScale = startBounds.height() / finalBounds.height()
      val startWidth: Float = startScale * finalBounds.width()
      val deltaWidth: Float = (startWidth - startBounds.width()) / 2
      startBounds.left -= deltaWidth.toInt()
      startBounds.right += deltaWidth.toInt()
    } else {
      // Extend start bounds vertically
      startScale = startBounds.width() / finalBounds.width()
      val startHeight: Float = startScale * finalBounds.height()
      val deltaHeight: Float = (startHeight - startBounds.height()) / 2f
      startBounds.top -= deltaHeight.toInt()
      startBounds.bottom += deltaHeight.toInt()
    }

    // Hide the thumbnail and show the zoomed-in view. When the animation
    // begins, it will position the zoomed-in view in the place of the
    // thumbnail.
    thumbView.alpha = 0f
    binding.expandedImage.show()

    // Set the pivot point for SCALE_X and SCALE_Y transformations
    // to the top-left corner of the zoomed-in view (the default
    // is the center of the view).
    binding.expandedImage.pivotX = 0f
    binding.expandedImage.pivotY = 0f

    // Construct and run the parallel animation of the four translation and
    // scale properties (X, Y, SCALE_X, and SCALE_Y).
    currentAnimator = AnimatorSet().apply {
      play(
        ObjectAnimator.ofFloat(
          binding.expandedImage,
          View.X,
          startBounds.left,
          finalBounds.left
        )
      ).apply {
        with(
          ObjectAnimator.ofFloat(
            binding.expandedImage,
            View.Y,
            startBounds.top,
            finalBounds.top
          )
        )
        with(ObjectAnimator.ofFloat(binding.expandedImage, View.SCALE_X, startScale, 1f))
        with(ObjectAnimator.ofFloat(binding.expandedImage, View.SCALE_Y, startScale, 1f))
      }
      duration = shortAnimationDuration.toLong()
      interpolator = DecelerateInterpolator()
      addListener(object : AnimatorListenerAdapter() {

        override fun onAnimationEnd(animation: Animator) {
          currentAnimator = null
        }

        override fun onAnimationCancel(animation: Animator) {
          currentAnimator = null
        }
      })
      start()
    }

    // Upon clicking the zoomed-in image, it should zoom back down
    // to the original bounds and show the thumbnail instead of
    // the expanded image.
    binding.expandedImage.setOnClickListener {
        hideExpandedImage()
    }
  }

  private fun hideExpandedImage() {
    currentAnimator?.cancel()

    // Animate the four positioning/sizing properties in parallel,
    // back to their original values.
    currentAnimator = AnimatorSet().apply {
      play(ObjectAnimator.ofFloat(binding.expandedImage, View.X, startBounds.left)).apply {
        with(ObjectAnimator.ofFloat(binding.expandedImage, View.Y, startBounds.top))
        with(ObjectAnimator.ofFloat(binding.expandedImage, View.SCALE_X, startScale))
        with(ObjectAnimator.ofFloat(binding.expandedImage, View.SCALE_Y, startScale))
      }
      duration = shortAnimationDuration.toLong()
      interpolator = DecelerateInterpolator()
      addListener(object : AnimatorListenerAdapter() {

        override fun onAnimationEnd(animation: Animator) {
          currentThumbView?.alpha = 1f
          binding.expandedImage.hide()
          currentAnimator = null
        }

        override fun onAnimationCancel(animation: Animator) {
          currentThumbView?.alpha = 1f
          binding.expandedImage.hide()
          currentAnimator = null
        }
      })
      start()
    }
  }



}